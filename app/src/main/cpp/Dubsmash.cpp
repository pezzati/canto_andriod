//
// Created by hamed on 10/4/18.
//
#include <jni.h>
#include <android/log.h>
#include <string>
#include <AndroidIO/SuperpoweredAndroidAudioIO.h>
#include <SuperpoweredSimple.h>
#include <SuperpoweredTimeStretching.h>
#include <SuperpoweredDecoder.h>
#include <SuperpoweredRecorder.h>

#define log_write __android_log_write
#define log_print __android_log_print

const char *TAG = "DubsmashCPP";

static SuperpoweredAndroidAudioIO *audioIO;
static SuperpoweredTimeStretching *stretching;
static SuperpoweredAudiopointerList *outputBuffers;
static SuperpoweredDecoder *decoder;
static SuperpoweredRecorder *recorder;
const char *outFilePath;
const char *tempFilePath;

static short int *intBuffer;
static int sampleRate;
static int bufferSize;

bool audioInitialized = false;
bool playing = false;

static bool audioProcessing(
        void *__unused clientData, // custom pointer
        short int *audio,           // buffer of interleaved samples
        int numberOfFrames,         // number of frames to process
        int __unused sampleRate     // sampling rate
) {

    if (playing) {
        unsigned int samplesDecoded = (unsigned int) numberOfFrames;
        if (decoder->decode(intBuffer, &samplesDecoded) == SUPERPOWEREDDECODER_ERROR) {
            log_print(ANDROID_LOG_DEBUG, TAG, "Decoder Error");
            return false;
        }
        if (samplesDecoded < 1) {
            __android_log_print(ANDROID_LOG_DEBUG, "STRETCHING", "BREAKED!!!");
            playing = false;
            return false;
        }

        log_print(ANDROID_LOG_DEBUG, TAG, "samplesDecoded=%d", samplesDecoded);

        SuperpoweredAudiobufferlistElement inputBuffer;
        inputBuffer.samplePosition = decoder->samplePosition;
        inputBuffer.startSample = 0;
        inputBuffer.samplesUsed = 0;
        inputBuffer.endSample = samplesDecoded;
        inputBuffer.buffers[0] = SuperpoweredAudiobufferPool::getBuffer(samplesDecoded * 8 + 64);
        inputBuffer.buffers[1] = inputBuffer.buffers[2] = inputBuffer.buffers[3] = NULL;

        SuperpoweredShortIntToFloat(intBuffer, (float *) inputBuffer.buffers[0], samplesDecoded);

        stretching->process(&inputBuffer, outputBuffers);

        if (outputBuffers->makeSlice(0, numberOfFrames)) {
            while (true) {
                int numSamples = 0;
                float *timeStretchedAudio = (float *) outputBuffers->nextSliceItem(&numSamples);
                if (!timeStretchedAudio) break;
//                recorder->process(timeStretchedAudio, (unsigned int) numSamples);
                SuperpoweredFloatToShortInt(timeStretchedAudio, audio,
                                            (unsigned int) numSamples);
                log_print(ANDROID_LOG_DEBUG, TAG, "numSamples=%d numberOfFrames=%d", numSamples,
                          numberOfFrames);
            };
            outputBuffers->clear();
            return true;
        };
    }
    return false;
}

static int getActualBufferSize() {
    short int *tempIntBuffer = (short int *) malloc(
            decoder->samplesPerFrame * 2 * sizeof(short int) + 32768);
    int maxSamples = 0;

    while (true) {
        unsigned int samplesDecoded = decoder->samplesPerFrame;
        if (decoder->decode(tempIntBuffer, &samplesDecoded) == SUPERPOWEREDDECODER_ERROR) {
            log_print(ANDROID_LOG_DEBUG, TAG, "Decoder Error");
            return 0;
        }
        if (samplesDecoded < 1) {
            log_print(ANDROID_LOG_DEBUG, TAG, "BREAKED!!!");
            return 0;
        }

        log_print(ANDROID_LOG_DEBUG, TAG, "samplesDecoded=%d", samplesDecoded);

        SuperpoweredAudiobufferlistElement inputBuffer;
        inputBuffer.samplePosition = decoder->samplePosition;
        inputBuffer.startSample = 0;
        inputBuffer.samplesUsed = 0;
        inputBuffer.endSample = samplesDecoded;
        inputBuffer.buffers[0] = SuperpoweredAudiobufferPool::getBuffer(samplesDecoded * 8 + 64);
        inputBuffer.buffers[1] = inputBuffer.buffers[2] = inputBuffer.buffers[3] = NULL;

        SuperpoweredShortIntToFloat(intBuffer, (float *) inputBuffer.buffers[0], samplesDecoded);

        stretching->process(&inputBuffer, outputBuffers);

        log_print(ANDROID_LOG_DEBUG, TAG, "outputBuffers->sampleLength=%d",
                  outputBuffers->sampleLength);

        if (outputBuffers->makeSlice(0, outputBuffers->sampleLength)) {
            while (true) {
                int numSamples = 0;
                float *timeStretchedAudio = (float *) outputBuffers->nextSliceItem(&numSamples);
                if (!timeStretchedAudio) break;
                if (numSamples > maxSamples) {
                    maxSamples = numSamples;
                }
            };

            outputBuffers->clear();
            if (maxSamples > 0) {
                free(tempIntBuffer);
                return maxSamples;
            }
        };
    };
}

// This is called after the recorder closed the WAV file.
static void recorderStopped(void *__unused clientdata) {
    log_write(ANDROID_LOG_DEBUG, "RecorderExample", "Finished recording.");
    delete recorder;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_InitAudio(
        JNIEnv  __unused *env,
        jobject  __unused obj,
        jint bSize,
        jint sRate,
        jstring outputPath,
        jstring tempPath
) {

    decoder = new SuperpoweredDecoder();


    outputBuffers = new SuperpoweredAudiopointerList(8, 16);

    outFilePath = env->GetStringUTFChars(outputPath, 0);
    tempFilePath = env->GetStringUTFChars(tempPath, 0);

    sampleRate = sRate;
    bufferSize = bSize;
}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_activities_DubsmashActivity_OpenFile(
        JNIEnv *env,
        jobject  __unused obj,
        jstring filePath) {

    const char *path = env->GetStringUTFChars(filePath, 0);

    decoder->open(path);

    log_print(ANDROID_LOG_DEBUG, TAG, "decode->samplesPerFrame=%d", decoder->samplesPerFrame);

    intBuffer = (short int *) malloc(decoder->samplesPerFrame * 2 * sizeof(short int) + 32768);

    stretching = new SuperpoweredTimeStretching(decoder->samplerate);

    stretching->setRateAndPitchShift(1, 0);

    int actualBufferSize = bufferSize; //getActualBufferSize();

    log_print(ANDROID_LOG_DEBUG, TAG, "bufferSize=%d", actualBufferSize);

    decoder->seek(0, true);

    audioIO = new SuperpoweredAndroidAudioIO(
            sampleRate,
            actualBufferSize,
            false,
            true,
            audioProcessing,
            NULL,
            -1, -1,
            actualBufferSize * 2
    );
    // Initialize the recorder with a temporary file path.
    recorder = new SuperpoweredRecorder(
            tempFilePath,               // The full filesystem path of a temporarily file.
            decoder->samplerate,   // Sampling rate.
            1,                  // The minimum length of a recording (in seconds).
            2,                  // The number of channels.
            false,              // applyFade (fade in/out at the beginning / end of the recording)
            recorderStopped,    // Called when the recorder finishes writing after stop().
            NULL                // A custom pointer your callback receives (clientData).
    );


    log_print(ANDROID_LOG_DEBUG, TAG, "decoder->sampleRate=%d, samplesPerFrame=%d",
              decoder->samplerate, decoder->samplesPerFrame);
    audioInitialized = true;
    return 0;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_TogglePlayback(
        JNIEnv *env,
        jobject  __unused obj) {
    playing = !playing;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_StartAudio(
        JNIEnv *env,
        jobject  __unused obj) {
    playing = true;
    recorder->start(outFilePath);
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_StopAudio(
        JNIEnv *env,
        jobject  __unused obj) {
    recorder->stop();
    playing = false;
}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_activities_DubsmashActivity_GetProgressMS(
        JNIEnv *env,
        jobject  __unused obj) {
    return 0;
}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_activities_DubsmashActivity_GetDurationMS(
        JNIEnv *env,
        jobject  __unused obj) {
    return 0;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_Seek(
        JNIEnv *env,
        jobject  __unused obj,
        jdouble positionMS) {

}

// onBackground - Put audio processing to sleep.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_MainActivity_onBackground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    if (audioInitialized)
        audioIO->onBackground();
}

// onForeground - Resume audio processing.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_MainActivity_onForeground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    if (audioInitialized)
        audioIO->onForeground();
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_MainActivity_Cleanup(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    delete audioIO;
    delete decoder;
}