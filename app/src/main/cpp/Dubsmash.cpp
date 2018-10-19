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
static float *playerBuffer;

bool audioInitialized = false;
bool playing = false;

static bool audioProcessing(
        void *__unused clientData, // custom pointer
        short int *audio,           // buffer of interleaved samples
        int numberOfFrames,         // number of frames to process
        int __unused sampleRate     // sampling rate
) {

    if (playing) {
        unsigned int samplesDecoded = decoder->samplesPerFrame;
        if (decoder->decode(intBuffer, &samplesDecoded) == SUPERPOWEREDDECODER_ERROR) return false;
        if (samplesDecoded < 1) {
            __android_log_print(ANDROID_LOG_DEBUG, "STRETCHING", "BREAKED!!!");
            playing = false;
            return false;
        }

//        log_print(ANDROID_LOG_DEBUG, TAG, "samplesDecoded=%d", samplesDecoded);

        SuperpoweredAudiobufferlistElement inputBuffer;
        inputBuffer.samplePosition = decoder->samplePosition;
        inputBuffer.startSample = 0;
        inputBuffer.samplesUsed = 0;
        inputBuffer.endSample = samplesDecoded;
        inputBuffer.buffers[0] = SuperpoweredAudiobufferPool::getBuffer(samplesDecoded * 8 + 64);
        inputBuffer.buffers[1] = inputBuffer.buffers[2] = inputBuffer.buffers[3] = NULL;

        // Convert the decoded PCM samples from 16-bit integer to 32-bit floating point.
        SuperpoweredShortIntToFloat(intBuffer, (float *) inputBuffer.buffers[0], samplesDecoded);

        stretching->process(&inputBuffer, outputBuffers);

        if (outputBuffers->makeSlice(0, outputBuffers->sampleLength)) {
            int tempNumSamples = 0;
            while (true) { // Iterate on every output slice.
                // Get pointer to the output samples.
                int numSamples = 0;
                float *timeStretchedAudio = (float *) outputBuffers->nextSliceItem(&numSamples);
                if (!timeStretchedAudio) break;
                // Convert the time stretched PCM samples from 32-bit floating point to 16-bit integer.
                SuperpoweredFloatToShortInt(timeStretchedAudio, intBuffer,
                                            (unsigned int) numSamples);
                SuperpoweredShortIntToFloat(intBuffer, playerBuffer, (unsigned int) numSamples);
//                tempNumSamples += numSamples;
                recorder->process(playerBuffer, (unsigned int) numSamples);
                SuperpoweredFloatToShortInt(playerBuffer, audio, (unsigned int) numSamples);
                log_print(ANDROID_LOG_DEBUG, TAG, "numSamples=%d, numFrames=%d, sampleRate=%d",
                          numSamples,
                          numberOfFrames, sampleRate);
            };


            outputBuffers->clear();
            return true;
        };
    }
    return false;
}

// This is called after the recorder closed the WAV file.
static void recorderStopped(void *__unused clientdata) {
    log_write(ANDROID_LOG_DEBUG, "RecorderExample", "Finished recording.");
    delete recorder;
}

extern "C" JNIEXPORT void
Java_com_example_activities_DubsmashActivity_InitAudio(
        JNIEnv  __unused *env,
        jobject  __unused obj,
        jint bufferSize,
        jint sampleRate,
        jstring outputPath,
        jstring tempPath
) {

    decoder = new SuperpoweredDecoder();


    outputBuffers = new SuperpoweredAudiopointerList(8, 16);

    outFilePath = env->GetStringUTFChars(outputPath, 0);
    tempFilePath = env->GetStringUTFChars(tempPath, 0);


}

extern "C" JNIEXPORT jdouble
Java_com_example_activities_DubsmashActivity_OpenFile(
        JNIEnv *env,
        jobject  __unused obj,
        jstring filePath) {
    const char *path = env->GetStringUTFChars(filePath, 0);
    decoder->open(path);
    intBuffer = (short int *) malloc(decoder->samplesPerFrame * 2 * sizeof(short int) + 32768);
    playerBuffer = (float *) malloc(decoder->samplesPerFrame * 2 * sizeof(short int) + 32768);
    audioIO = new SuperpoweredAndroidAudioIO(
            decoder->samplerate,
            decoder->samplesPerFrame,
            false,
            true,
            audioProcessing,
            NULL,
            -1, -1,
            decoder->samplesPerFrame * 2
    );

    stretching = new SuperpoweredTimeStretching(decoder->samplerate);

    stretching->setRateAndPitchShift(1, 0);

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
Java_com_example_activities_DubsmashActivity_TogglePlayback(
        JNIEnv *env,
        jobject  __unused obj) {
    playing = !playing;
}

extern "C" JNIEXPORT void
Java_com_example_activities_DubsmashActivity_StartAudio(
        JNIEnv *env,
        jobject  __unused obj) {
    playing = true;
    recorder->start(outFilePath);
}

extern "C" JNIEXPORT void
Java_com_example_activities_DubsmashActivity_StopAudio(
        JNIEnv *env,
        jobject  __unused obj) {
    recorder->stop();
    playing = false;
}

extern "C" JNIEXPORT jdouble
Java_com_example_activities_DubsmashActivity_GetProgressMS(
        JNIEnv *env,
        jobject  __unused obj) {
    return 0;
}

extern "C" JNIEXPORT jdouble
Java_com_example_activities_DubsmashActivity_GetDurationMS(
        JNIEnv *env,
        jobject  __unused obj) {
    return 0;
}

extern "C" JNIEXPORT void
Java_com_example_activities_DubsmashActivity_Seek(
        JNIEnv *env,
        jobject  __unused obj,
        jdouble positionMS) {

}

// onBackground - Put audio processing to sleep.
extern "C" JNIEXPORT void
Java_com_example_MainActivity_onBackground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    if (audioInitialized)
        audioIO->onBackground();
}

// onForeground - Resume audio processing.
extern "C" JNIEXPORT void
Java_com_example_MainActivity_onForeground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    if (audioInitialized)
        audioIO->onForeground();
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_example_MainActivity_Cleanup(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    delete audioIO;
    delete decoder;
    free(playerBuffer);
}