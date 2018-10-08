#include <tar.h>//
// Created by hamed on 10/4/18.
//
#include <jni.h>
#include <android/log.h>
#include <string>
#include <AndroidIO/SuperpoweredAndroidAudioIO.h>
#include <SuperpoweredSimple.h>
#include <SuperpoweredTimeStretching.h>
#include <SuperpoweredDecoder.h>

#define log_write __android_log_write
#define log_print __android_log_print

static SuperpoweredAndroidAudioIO *audioIO;
static SuperpoweredTimeStretching *stretching;
static SuperpoweredAudiopointerList *outputBuffers;
static SuperpoweredDecoder *decoder;

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
            return false;
        }

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

            while (true) { // Iterate on every output slice.
                // Get pointer to the output samples.
                int numSamples = 0;
                float *timeStretchedAudio = (float *) outputBuffers->nextSliceItem(&numSamples);
                if (!timeStretchedAudio) break;
                // Convert the time stretched PCM samples from 32-bit floating point to 16-bit integer.
                SuperpoweredFloatToShortInt(timeStretchedAudio, intBuffer,
                                            (unsigned int) numSamples);
                SuperpoweredShortIntToFloat(intBuffer, playerBuffer, (unsigned int) numSamples);
            };
            SuperpoweredFloatToShortInt(playerBuffer, audio, (unsigned int) numberOfFrames);
            // Clear the output buffer list.
            outputBuffers->clear();
            return true;
        };
    }
    return false;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_PlayerActivity_InitAudio(
        JNIEnv  __unused *env,
        jobject  __unused obj,
        jint bufferSize,
        jint sampleRate
) {

    decoder = new SuperpoweredDecoder();

    stretching = new SuperpoweredTimeStretching((unsigned int) sampleRate);

    stretching->setRateAndPitchShift(1, 0);

    outputBuffers = new SuperpoweredAudiopointerList(8, 16);

}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_PlayerActivity_OpenFile(
        JNIEnv *env,
        jobject  __unused obj,
        jstring filePath,
        jint offset,        // offset of audio file
        jint length) {
    const char *path = env->GetStringUTFChars(filePath, 0);
    decoder->open(path);
    intBuffer = (short int *) malloc(decoder->samplesPerFrame * 2 * sizeof(short int));
    playerBuffer = (float *) malloc(decoder->samplesPerFrame * 2 * sizeof(short int));
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
    audioInitialized = true;
    return 0;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_PlayerActivity_TogglePlayback(
        JNIEnv *env,
        jobject  __unused obj) {
    playing = !playing;

}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_PlayerActivity_GetProgressMS(
        JNIEnv *env,
        jobject  __unused obj) {
    return 0;
}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_PlayerActivity_GetDurationMS(
        JNIEnv *env,
        jobject  __unused obj) {
    return 0;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_PlayerActivity_Seek(
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
    free(playerBuffer);
}