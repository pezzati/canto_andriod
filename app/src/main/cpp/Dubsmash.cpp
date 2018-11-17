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

const char *TAG = "DubsmashCPP";

static SuperpoweredAndroidAudioIO *audioIO;
static SuperpoweredAdvancedAudioPlayer *player;
static SuperpoweredRecorder *recorder;
static SuperpoweredRecorder *micRecorder;

const char *outFilePath;
const char *tempFilePath;

const char *outFilePathMic;
const char *tempFilePathMic;

bool isSinging = false;

static float *playerBuffer;
static float *micBuffer;

bool audioInitialized = false;
bool playing = false;

static bool audioProcessing(
        void *__unused clientData, // custom pointer
        short int *audio,           // buffer of interleaved samples
        int numberOfFrames,         // number of frames to process
        int __unused sampleRate     // sampling rate
) {

    if (isSinging) {
        SuperpoweredShortIntToFloat(audio, micBuffer, (unsigned int) numberOfFrames);
        micRecorder->process(micBuffer, (unsigned int) numberOfFrames);
    }

    if (player->playing && player->process(playerBuffer, false, (unsigned int) numberOfFrames, 1)) {
        recorder->process(playerBuffer, (unsigned int) numberOfFrames);
        SuperpoweredFloatToShortInt(playerBuffer, audio, (unsigned int) numberOfFrames);
        return true;
    }

    return false;
}

// This is called after the recorder closed the WAV file.
static void recorderStopped(void *__unused clientdata) {
    log_write(ANDROID_LOG_DEBUG, "RecorderExample", "Finished recording.");
    delete recorder;
}

// Called by the player.
static void playerEventCallback(
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
            //player->seek(0);    // loop track
            break;
    };
}


extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_InitAudio(
        JNIEnv *env,
        jobject  __unused jobj,
        jint bufferSize,
        jint sampleRate,
        jboolean isSing,
        jstring outputPath,
        jstring tempPath,
        jstring outputPathMic,
        jstring tempPathMic
) {
    isSinging = isSing;
    outFilePath = env->GetStringUTFChars(outputPath, 0);
    tempFilePath = env->GetStringUTFChars(tempPath, 0);

    outFilePathMic = env->GetStringUTFChars(outputPathMic, 0);
    tempFilePathMic = env->GetStringUTFChars(tempPathMic, 0);

    playerBuffer = (float *) malloc(sizeof(float) * 2 * bufferSize);
    micBuffer = (float *) malloc(sizeof(float) * 2 * bufferSize);

    recorder = new SuperpoweredRecorder(
            tempFilePath,
            (unsigned int) sampleRate,
            1,
            2,
            false,
            recorderStopped,
            NULL
    );

    if (isSinging) {
        micRecorder = new SuperpoweredRecorder(
                tempFilePathMic,
                (unsigned int) sampleRate,
                1,
                2,
                false,
                recorderStopped,
                NULL
        );
    }

    player = new SuperpoweredAdvancedAudioPlayer(
            env,
            playerEventCallback,
            (unsigned int) sampleRate,
            0, 2, 3
    );

    audioIO = new SuperpoweredAndroidAudioIO(
            sampleRate,
            bufferSize,
            isSinging,
            true,
            audioProcessing,
            NULL,
            -1, -1,
            bufferSize * 2
    );


}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_activities_DubsmashActivity_OpenFile(
        JNIEnv *env,
        jobject  __unused obj,
        jstring filePath,
        jint length) {

    const char *path = env->GetStringUTFChars(filePath, 0);

    player->open(path, 0, length);

    audioInitialized = true;
    return 0;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_TogglePlayback(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    player->togglePlayback();
    SuperpoweredCPU::setSustainedPerformanceMode(player->playing);  // prevent dropouts
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_StartAudio(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    if (isSinging) {
        micRecorder->start(outFilePathMic);
    }
    recorder->start(outFilePath);
    player->play(false);
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_StopAudio(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    if (isSinging) {
        micRecorder->stop();
    }
    recorder->stop();
    player->pause();
}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_activities_DubsmashActivity_GetProgressMS(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    return player->positionMs;
}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_activities_DubsmashActivity_GetDurationMS(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    return player->durationMs;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_Seek(
        JNIEnv  __unused *env,
        jobject  __unused obj,
        jdouble percent) {
    player->seek(percent);
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_SeekMS(
        JNIEnv  __unused *env,
        jobject  __unused obj,
        jdouble position) {
    player->setPosition(position, false, false);
}

// onBackground - Put audio processing to sleep.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_DubsmashActivity_onBackground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    if (audioInitialized)
        audioIO->onBackground();
}

// onForeground - Resume audio processing.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_DubsmashActivity_onForeground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    if (audioInitialized)
        audioIO->onForeground();
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_DubsmashActivity_Cleanup(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    delete audioIO;
    delete player;
    delete obj;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_SetPitch(
        JNIEnv *__unused env,
        jobject __unused obj,
        jint pitch
) {
    player->setPitchShift(pitch);
}


extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_DubsmashActivity_SetTempo(
        JNIEnv *__unused env,
        jobject __unused obj,
        jdouble tempo
) {
    player->setTempo(tempo, true);
}