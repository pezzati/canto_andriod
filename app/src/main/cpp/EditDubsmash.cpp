#include <tar.h>//
// Created by hamed on 10/4/18.
//
#include <jni.h>
#include <android/log.h>
#include <string>
#include <AndroidIO/SuperpoweredAndroidAudioIO.h>
#include <SuperpoweredTimeStretching.h>
#include <SuperpoweredDecoder.h>
#include <SuperpoweredRecorder.h>
#include <SuperpoweredAdvancedAudioPlayer.h>
#include <SuperpoweredCPU.h>
#include <SuperpoweredSimple.h>

#define log_write __android_log_write
#define log_print __android_log_print

static SuperpoweredAndroidAudioIO *audioIO;
static SuperpoweredAdvancedAudioPlayer *player;
static SuperpoweredDecoder *decoder;

static float *playerBuffer;

static bool ed_audioProcessing(
        void *__unused clientData, // custom pointer
        short int *audio,           // buffer of interleaved samples
        int numberOfFrames,         // number of frames to process
        int __unused sampleRate     // sampling rate
) {
    if (player->playing && player->process(playerBuffer, false, (unsigned int) numberOfFrames, 1)) {
        SuperpoweredFloatToShortInt(playerBuffer, audio, (unsigned int) numberOfFrames);
        return true;
    }

    return false;
}


// Called by the player.
static void ed_playerEventCallback(
        void __unused *clientData,
        SuperpoweredAdvancedAudioPlayerEvent event,
        void *value
) {
    switch (event) {
        case SuperpoweredAdvancedAudioPlayerEvent_LoadSuccess:
            break;
        case SuperpoweredAdvancedAudioPlayerEvent_LoadError:
            log_print(ANDROID_LOG_ERROR, "PlayerExample", "Open error: %s", (char *) value);
            break;
        case SuperpoweredAdvancedAudioPlayerEvent_EOF:
            player->pause(0, 0);
            //player->seek(0);    // loop track
            break;
    };
}


extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_InitAudio(
        JNIEnv *env,
        jobject  __unused jobj,
        jint bufferSize,
        jint sampleRate
) {

    playerBuffer = (float *) malloc(sizeof(float) * 2 * bufferSize);
    player = new SuperpoweredAdvancedAudioPlayer(
            env,
            ed_playerEventCallback,
            (unsigned int) sampleRate,
            0, 2, 3
    );
    audioIO = new SuperpoweredAndroidAudioIO(
            sampleRate,
            bufferSize,
            false,
            true,
            ed_audioProcessing,
            NULL,
            -1, -1,
            bufferSize * 2
    );

    decoder = new SuperpoweredDecoder();
}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_activities_EditActivity_OpenFile(
        JNIEnv *env,
        jobject  __unused obj,
        jstring filePath,
        jint length) {

    const char *path = env->GetStringUTFChars(filePath, 0);

    player->open(path, 0, length);

    return player->durationMs;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_TogglePlayback(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    player->togglePlayback();
    SuperpoweredCPU::setSustainedPerformanceMode(player->playing);  // prevent dropouts
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_StartAudio(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    player->play(false);
    SuperpoweredCPU::setSustainedPerformanceMode(true);
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_StopAudio(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    player->pause();
    SuperpoweredCPU::setSustainedPerformanceMode(false);
}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_activities_EditActivity_GetProgressMS(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    return player->positionMs;
}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_activities_EditActivity_GetDurationMS(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    return player->durationMs;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_Seek(
        JNIEnv  __unused *env,
        jobject  __unused obj,
        jdouble percent) {
    player->seek(percent);
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_SeekMS(
        JNIEnv  __unused *env,
        jobject  __unused obj,
        jdouble position) {
    player->setPosition(position, false, false);
}

// onBackground - Put audio processing to sleep.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_onBackground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    audioIO->onBackground();
}

// onForeground - Resume audio processing.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_onForeground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    audioIO->onForeground();
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_Cleanup(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    delete audioIO;
    delete player;
    delete obj;
}

extern "C" JNIEXPORT jboolean
Java_com_hmomeni_canto_activities_EditActivity_IsPlaying(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    return player->playing ? (u_int8_t) 1 : (u_int8_t) 0;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_CropSave(
        JNIEnv *__unused env,
        jobject __unused obj,
        jstring sourceFile,
        jstring destFile,
        jlong from,
        jlong to,
        jlong total
) {

    const char *sourceFilePath = env->GetStringUTFChars(sourceFile, 0);
    const char *destFilePath = env->GetStringUTFChars(destFile, 0);

    const char *openError = decoder->open(sourceFilePath);

    if (openError) {
        log_print(ANDROID_LOG_DEBUG, "EditDub", "Open error: %s", openError);
        delete decoder;
        return;
    };

    double fromSample = (float) from * (float) decoder->durationSamples / total;
    double toSample = (float) to * (float) decoder->durationSamples / total;

    short int *intBuffer = (short int *) malloc(
            decoder->samplesPerFrame * 2 * sizeof(short int) + 32768);

    FILE *fd = createWAV(destFilePath, decoder->samplerate, 2);

    decoder->seek((int64_t) fromSample, true);


    while (true) {
        unsigned int samplesDecoded = decoder->samplesPerFrame;
        if (decoder->decode(intBuffer, &samplesDecoded) == SUPERPOWEREDDECODER_ERROR) break;

        if (samplesDecoded < 1) {
            break;
        }

        fwrite(intBuffer, 1, samplesDecoded * 4, fd);

        if (decoder->samplePosition >= toSample) {
            break;
        }
    }

    closeWAV(fd);
}