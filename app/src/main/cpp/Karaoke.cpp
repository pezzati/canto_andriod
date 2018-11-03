//
// Created by hamed on 10/31/18.
//
#include <jni.h>
#include <android/log.h>
#include <string>
#include <AndroidIO/SuperpoweredAndroidAudioIO.h>
#include <SuperpoweredSimple.h>
#include <SuperpoweredAdvancedAudioPlayer.h>
#include <SuperpoweredCPU.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <SLES/OpenSLES.h>
#include <SuperpoweredRecorder.h>
#include <SuperpoweredMixer.h>
#include <SuperpoweredReverb.h>

#define log_write __android_log_write
#define log_print __android_log_print

const char *KTAG = "KaraokeCPP";

static SuperpoweredAndroidAudioIO *audioIO;
static SuperpoweredAdvancedAudioPlayer *player;
static SuperpoweredStereoMixer *mixer;
static SuperpoweredReverb *reverb;

static float *micBuffer;
static float *reverbBuffer;
static float *playerBuffer;
static float *outputBuffer;
static float *inputs[4];
static float *outputs[2];

static float inputLevels[8] = {1, 1, 1, 1, 0, 0, 0, 0};
static float outputLevels[2] = {1, 1};

static float volume = 1;

static bool audioProcessing(
        void *__unused clientdata, // custom pointer
        short int *audio,           // buffer of interleaved samples
        int numberOfFrames,         // number of frames to process
        int __unused samplerate     // sampling rate
) {
    SuperpoweredShortIntToFloat(audio, micBuffer, (unsigned int) numberOfFrames);
    SuperpoweredVolume(micBuffer, micBuffer, 3, 3, (unsigned int) numberOfFrames);

    if (reverb->enabled && player->playing) {
        bool haveMusic = player->process(playerBuffer, false, (unsigned int) numberOfFrames,
                                         volume);
        bool haveReverb = reverb->process(micBuffer, reverbBuffer, (unsigned int) numberOfFrames);
        outputs[0] = outputBuffer;
        outputs[1] = NULL;
        inputs[2] = inputs[3] = NULL;
        if (haveMusic) {
            inputs[0] = playerBuffer;
        } else {
            inputs[0] = NULL;
        }
        if (haveReverb) {
            inputs[1] = reverbBuffer;
        } else {
            inputs[1] = NULL;
        }
        mixer->process(inputs, outputs, inputLevels, outputLevels, NULL, NULL,
                       (unsigned int) numberOfFrames);
        SuperpoweredFloatToShortInt(outputBuffer, audio, (unsigned int) numberOfFrames);
        if (haveMusic || haveReverb) {
            return true;
        }
    } else if (reverb->enabled && !player->playing) {
        if (reverb->process(micBuffer, reverbBuffer, (unsigned int) numberOfFrames)) {
            SuperpoweredFloatToShortInt(reverbBuffer, audio, (unsigned int) numberOfFrames);
            return true;
        }
    } else if (!reverb->enabled && player->playing) {
        bool haveMusic = player->process(playerBuffer, false, (unsigned int) numberOfFrames,
                                         volume);
        outputs[0] = outputBuffer;
        outputs[1] = NULL;
        inputs[2] = inputs[3] = NULL;
        if (haveMusic) {
            inputs[0] = playerBuffer;
        } else {
            inputs[0] = NULL;
        }
        inputs[1] = micBuffer;
        mixer->process(inputs, outputs, inputLevels, outputLevels, NULL, NULL,
                       (unsigned int) numberOfFrames);
        SuperpoweredFloatToShortInt(outputBuffer, audio, (unsigned int) numberOfFrames);
        return true;
    } else {
        SuperpoweredFloatToShortInt(micBuffer, audio, (unsigned int) numberOfFrames);
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


// StartAudio - Start audio engine and initialize player.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_KaraokeActivity_StartAudio(
        JNIEnv *__unused env,
        jobject  __unused obj,
        jint samplerate,
        jint buffersize
) {
    // Allocate audio buffer.
    playerBuffer = (float *) malloc(sizeof(float) * 2 * buffersize);
    micBuffer = (float *) malloc(sizeof(float) * 2 * buffersize);
    reverbBuffer = (float *) malloc(sizeof(float) * 2 * buffersize);
    outputBuffer = (float *) malloc(sizeof(float) * 2 * buffersize);

    mixer = new SuperpoweredStereoMixer();

    // Initialize player and pass callback function.
    player = new SuperpoweredAdvancedAudioPlayer(
            NULL,                           // clientData
            playerEventCallback,            // callback function
            (unsigned int) samplerate,       // sampling rate
            0                               // cachedPointCount
    );

    reverb = new SuperpoweredReverb((unsigned int) samplerate);

    audioIO = new SuperpoweredAndroidAudioIO(
            samplerate,                     // sampling rate
            buffersize,                     // buffer size
            true,                           // enableInput
            true,                           // enableOutput
            audioProcessing,                // process callback function
            NULL,                           // clientData
            -1,                             // inputStreamType (-1 = default)
            SL_ANDROID_STREAM_MEDIA,        // outputStreamType (-1 = default)
            buffersize * 2                  // latencySamples
    );
}

// OpenFile - Open file in player, specifying offset and length.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_KaraokeActivity_OpenFile(
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
Java_com_hmomeni_canto_activities_KaraokeActivity_TogglePlayback(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    player->togglePlayback();
    SuperpoweredCPU::setSustainedPerformanceMode(player->playing);  // prevent dropouts
}

// onBackground - Put audio processing to sleep.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_KaraokeActivity_onBackground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    audioIO->onBackground();
}

// onForeground - Resume audio processing.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_KaraokeActivity_onForeground(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    audioIO->onForeground();
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_KaraokeActivity_Cleanup(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    delete audioIO;
    delete player;
    free(playerBuffer);
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_KaraokeActivity_SetPitch(
        JNIEnv *__unused env,
        jobject __unused obj,
        jint pitch
) {
    player->setPitchShift(pitch);
}


// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_KaraokeActivity_SetTempo(
        JNIEnv *__unused env,
        jobject __unused obj,
        jdouble tempo
) {
    player->setTempo(tempo, true);
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_KaraokeActivity_SetVolume(
        JNIEnv *__unused env,
        jobject __unused obj,
        jfloat vol
) {
    volume = vol;
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_KaraokeActivity_SetReverb(
        JNIEnv *__unused env,
        jobject __unused obj,
        jfloat reverbAmount
) {
    if (reverbAmount == 0) {
        reverb->enable(false);
    } else {
        if (!reverb->enabled) {
            reverb->enable(true);
        }
        reverb->setMix(reverbAmount);
    }
}

// Cleanup - Free resources.
extern "C" JNIEXPORT jboolean
Java_com_hmomeni_canto_activities_KaraokeActivity_IsPlaying(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    if (player->playing) {
        return 1;
    } else {
        return 0;
    }
}
