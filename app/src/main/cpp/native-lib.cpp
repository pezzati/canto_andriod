#include <jni.h>
#include <AndroidIO/SuperpoweredAndroidAudioIO.h>
#include <android/log.h>
#include <string>
#include <SuperpoweredAdvancedAudioPlayer.h>
#include <SuperpoweredCPU.h>
#include <SuperpoweredSimple.h>
#include <SuperpoweredMixer.h>
#include <SuperpoweredRecorder.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <SLES/OpenSLES.h>

#define log_write __android_log_write
#define log_print __android_log_print

static SuperpoweredAndroidAudioIO *audioIO;
static SuperpoweredAdvancedAudioPlayer *player;
static SuperpoweredStereoMixer *mixer;
static SuperpoweredRecorder *recorder;
static float inputLevels[8] = {1, 1, 1, 1, 0, 0, 0, 0};
static float outputLevels[2] = {1, 1};


static char *LOG_TAG = const_cast<char *>("CANTO-JNI");
static float *playerBuffer;
static float *micBuffer;
static bool isRecording = false;

static bool audioProcessing(
        void *__unused clientdata, // custom pointer
        short int *audio,           // buffer of interleaved samples
        int numberOfFrames,         // number of frames to process
        int __unused samplerate     // sampling rate
) {

    SuperpoweredShortIntToFloat(audio, micBuffer, (unsigned int) numberOfFrames);

    float *inputs[4];
    float *outputs[2];

    inputs[0] = micBuffer;

    if (player->process(playerBuffer, false, (unsigned int) numberOfFrames)) {
        SuperpoweredFloatToShortInt(playerBuffer, audio, (unsigned int) numberOfFrames);
        inputs[1] = playerBuffer;
        outputs[0] = (float *) malloc(sizeof(float) * 2 * numberOfFrames);

        mixer->process(inputs, outputs, inputLevels, outputLevels, NULL, NULL,
                       (unsigned int) numberOfFrames);

        recorder->process(outputs[0], (unsigned int) numberOfFrames);

        SuperpoweredFloatToShortInt(playerBuffer, audio, (unsigned int) numberOfFrames);
        return true;
    }

    return false;

}

// Called by the player.
static void playerEventCallback(
        void *__unused clientData,
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
            player->seek(0);    // loop track
            break;
        default:;
    };
}

// This is called after the recorder closed the WAV file.
static void recorderStopped(void *__unused clientdata) {
    log_write(ANDROID_LOG_DEBUG, "RecorderExample", "Finished recording.");
    delete recorder;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_MainActivity_startAudio(
        JNIEnv *__unused env,
        jobject  __unused obj,
        jint sampleRate,
        jint bufferSize,
        jstring tempPath) {

    log_write(ANDROID_LOG_DEBUG, LOG_TAG, "Starting Audio");
    player = new SuperpoweredAdvancedAudioPlayer(
            NULL,                           // clientData
            playerEventCallback,            // callback function
            (unsigned int) sampleRate,       // sampling rate
            0                               // cachedPointCount
    );

    mixer = new SuperpoweredStereoMixer();

    // Get path strings.
    const char *temp = env->GetStringUTFChars(tempPath, 0);

    // Initialize the recorder with a temporary file path.
    recorder = new SuperpoweredRecorder(
            temp,               // The full filesystem path of a temporarily file.
            (unsigned int) sampleRate,   // Sampling rate.
            1,                  // The minimum length of a recording (in seconds).
            2,                  // The number of channels.
            false,              // applyFade (fade in/out at the beginning / end of the recording)
            recorderStopped,    // Called when the recorder finishes writing after stop().
            NULL                // A custom pointer your callback receives (clientData).
    );

    playerBuffer = (float *) malloc(sizeof(float) * 2 * bufferSize);
    micBuffer = (float *) malloc(sizeof(float) * 2 * bufferSize);
    audioIO = new SuperpoweredAndroidAudioIO(
            sampleRate,
            bufferSize,
            true,
            true,
            audioProcessing,
            NULL,
            -1,                             // inputStreamType (-1 = default)
            SL_ANDROID_STREAM_MEDIA,        // outputStreamType (-1 = default)
            bufferSize * 2
    );

}


// OpenFile - Open file in player, specifying offset and length.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_MainActivity_OpenFile(
        JNIEnv *env,
        jobject __unused obj,
        jstring path,       // path to APK file
        jint offset,        // offset of audio file
        jint length         // length of audio file
) {
    const char *str = env->GetStringUTFChars(path, 0);
    player->open(str, offset, length);
    env->ReleaseStringUTFChars(path, str);
}

// TogglePlayback - Toggle Play/Pause state of the player.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_MainActivity_TogglePlayback(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    player->togglePlayback();
    SuperpoweredCPU::setSustainedPerformanceMode(player->playing);  // prevent dropouts
}

// onBackground - Put audio processing to sleep.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_MainActivity_onBackground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    audioIO->onBackground();
}

// onForeground - Resume audio processing.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_MainActivity_onForeground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    audioIO->onForeground();
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_MainActivity_Cleanup(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    delete audioIO;
    delete player;
    free(playerBuffer);
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_MainActivity_StartRecording(
        JNIEnv *__unused env,
        jobject __unused obj,
        jstring destPath
) {
    const char *dest = env->GetStringUTFChars(destPath, 0);

    log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Started recording: %s", dest);

    recorder->start(dest);
    isRecording = true;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_MainActivity_StopRecording(
        JNIEnv *__unused env,
        jobject __unused obj
) {

    log_write(ANDROID_LOG_DEBUG, LOG_TAG, "Stopped recording!");
    isRecording = false;
    recorder->stop();
}