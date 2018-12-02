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
#include <SuperpoweredMixer.h>
#include <SuperpoweredReverb.h>
#include <SuperpoweredFlanger.h>

#define log_write __android_log_write
#define log_print __android_log_print

static SuperpoweredAndroidAudioIO *audioIO;
static SuperpoweredAdvancedAudioPlayer *player;
static SuperpoweredAdvancedAudioPlayer *micPlayer;
static SuperpoweredStereoMixer *mixer;
static SuperpoweredReverb *reverb;
static SuperpoweredFlanger *flanger;

static float *playerBuffer;
static float *micBuffer;
static float *floatBuffer;
bool ed_isSinging = false;

float iLevels[] = {1, 1, 4, 4};
float oLevels[] = {1, 1};

static int sampleRate, bufferSize;

static int appliedEffect = 0;


static bool ed_audioProcessing(
        void *__unused clientData, // custom pointer
        short int *audio,           // buffer of interleaved samples
        int numberOfFrames,         // number of frames to process
        int __unused sampleRate     // sampling rate
) {
    if (ed_isSinging) {
        if (player->playing) {
            bool hasMusic = player->process(playerBuffer, false, (unsigned int) numberOfFrames);
            bool hasMic = micPlayer->process(micBuffer, false, (unsigned int) numberOfFrames);

            if (hasMic && appliedEffect == 1) {
                hasMic = reverb->process(micBuffer, micBuffer, (unsigned int) numberOfFrames);
            }
            if (hasMic && appliedEffect == 2) {
                hasMic = flanger->process(micBuffer, micBuffer, (unsigned int) numberOfFrames);
            }
            if (hasMusic || hasMic) {
                float *inputs[4];
                inputs[0] = playerBuffer;
                inputs[1] = micBuffer;
                inputs[2] = inputs[3] = NULL;

                float *outputs[2];
                outputs[0] = floatBuffer;
                outputs[1] = NULL;

                mixer->process(inputs, outputs, iLevels, oLevels, NULL, NULL,
                               (unsigned int) numberOfFrames);
                SuperpoweredFloatToShortInt(floatBuffer, audio, (unsigned int) numberOfFrames);
                return true;
            }
        }
    } else {
        if (player->playing &&
            player->process(playerBuffer, false, (unsigned int) numberOfFrames, 1)) {
            SuperpoweredFloatToShortInt(playerBuffer, audio, (unsigned int) numberOfFrames);
            return true;
        }
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
        jint bSize,
        jint sRate,
        jboolean isSing
) {

    sampleRate = sRate;
    bufferSize = bSize;

    ed_isSinging = isSing;

    playerBuffer = (float *) malloc(sizeof(float) * 2 * bufferSize);
    player = new SuperpoweredAdvancedAudioPlayer(
            env,
            ed_playerEventCallback,
            (unsigned int) sampleRate,
            0, 2, 3
    );
    if (isSing) {
        mixer = new SuperpoweredStereoMixer();
        micBuffer = (float *) malloc(sizeof(float) * 2 * bufferSize);
        floatBuffer = (float *) malloc(sizeof(float) * 2 * bufferSize);
        micPlayer = new SuperpoweredAdvancedAudioPlayer(
                env,
                ed_playerEventCallback,
                (unsigned int) sampleRate,
                0, 2, 3
        );


    }

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

}

extern "C" JNIEXPORT jdouble
Java_com_hmomeni_canto_activities_EditActivity_OpenFile(
        JNIEnv *env,
        jobject  __unused obj,
        jstring filePath,
        jint length,
        jstring micFilePath,
        jint micLength) {

    const char *path = env->GetStringUTFChars(filePath, 0);

    player->open(path, 0, length);

    if (ed_isSinging) {
        const char *micPath = env->GetStringUTFChars(micFilePath, 0);
        micPlayer->open(micPath, 0, micLength);
    }

    return player->durationMs;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_TogglePlayback(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    player->togglePlayback();
    if (ed_isSinging)
        micPlayer->togglePlayback();
    SuperpoweredCPU::setSustainedPerformanceMode(player->playing);  // prevent dropouts
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_StartAudio(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    player->play(false);
    if (ed_isSinging)
        micPlayer->play(false);
    SuperpoweredCPU::setSustainedPerformanceMode(true);
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_StopAudio(
        JNIEnv  __unused *env,
        jobject  __unused obj) {
    player->pause();
    if (ed_isSinging)
        micPlayer->pause();
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
    if (ed_isSinging)
        micPlayer->seek(percent);
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_SeekMS(
        JNIEnv  __unused *env,
        jobject  __unused obj,
        jdouble position) {
    player->setPosition(position, false, false);
    if (ed_isSinging)
        micPlayer->setPosition(position, false, false);
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
    delete micPlayer;
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
    SuperpoweredDecoder *decoder = new SuperpoweredDecoder();

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


extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_SaveEffect(
        JNIEnv *__unused env,
        jobject __unused obj,
        jstring sourceFile,
        jstring destFile
) {

    SuperpoweredAudiopointerList *outputBuffers;

    if (appliedEffect == 3) // we only need this for time stretching and pitch shift
        outputBuffers = new SuperpoweredAudiopointerList(8, 16);

    SuperpoweredDecoder *decoder = new SuperpoweredDecoder();

    const char *sourceFilePath = env->GetStringUTFChars(sourceFile, 0);
    const char *destFilePath = env->GetStringUTFChars(destFile, 0);

    const char *openError = decoder->open(sourceFilePath);

    if (openError) {
        log_print(ANDROID_LOG_DEBUG, "EditDub", "Open error: %s", openError);
        delete decoder;
        return;
    };

    short int *intBuffer = (short int *) malloc(
            decoder->samplesPerFrame * 2 * sizeof(short int) + 32768);

    float *floatBuffer = (float *) malloc(
            decoder->samplesPerFrame * 2 * sizeof(floatBuffer) + 32768);

    FILE *fd = createWAV(destFilePath, decoder->samplerate, 2);


    while (true) {
        unsigned int samplesDecoded = decoder->samplesPerFrame;
        if (decoder->decode(intBuffer, &samplesDecoded) == SUPERPOWEREDDECODER_ERROR) break;
        switch (appliedEffect) {
            case 1: {
                SuperpoweredShortIntToFloat(intBuffer, floatBuffer, samplesDecoded);
                reverb->process(floatBuffer, floatBuffer, samplesDecoded);
                SuperpoweredFloatToShortInt(floatBuffer, intBuffer, samplesDecoded);
                break;
            }
            case 2: {
                SuperpoweredShortIntToFloat(intBuffer, floatBuffer, samplesDecoded);
                flanger->process(floatBuffer, floatBuffer, samplesDecoded);
                SuperpoweredFloatToShortInt(floatBuffer, intBuffer, samplesDecoded);
                break;
            }
            case 3: {
                SuperpoweredAudiobufferlistElement inputBuffer;
                inputBuffer.samplePosition = decoder->samplePosition;
                inputBuffer.startSample = 0;
                inputBuffer.samplesUsed = 0;
                inputBuffer.endSample = samplesDecoded; // <-- Important!
                inputBuffer.buffers[0] = SuperpoweredAudiobufferPool::getBuffer(
                        samplesDecoded * 8 + 64);
                inputBuffer.buffers[1] = inputBuffer.buffers[2] = inputBuffer.buffers[3] = NULL;

                SuperpoweredTimeStretching *timeStretch = new SuperpoweredTimeStretching(
                        decoder->samplerate);
                timeStretch->setRateAndPitchShift(1.0, 5);

                SuperpoweredShortIntToFloat(intBuffer, (float *) inputBuffer.buffers[0],
                                            samplesDecoded);

                timeStretch->process(&inputBuffer, outputBuffers);

                if (outputBuffers->makeSlice(0, outputBuffers->sampleLength)) {

                    while (true) { // Iterate on every output slice.
                        // Get pointer to the output samples.
                        int numSamples = 0;
                        float *timeStretchedAudio = (float *) outputBuffers->nextSliceItem(
                                &numSamples);
                        if (!timeStretchedAudio) break;

                        // Convert the time stretched PCM samples from 32-bit floating point to 16-bit integer.
                        SuperpoweredFloatToShortInt(timeStretchedAudio, intBuffer,
                                                    (unsigned int) numSamples);

                        // Write the audio to disk.
                        fwrite(intBuffer, 1, (unsigned int) numSamples * 4, fd);
                    };

                    // Clear the output buffer list.
                    outputBuffers->clear();
                };
            }
            default:
                break;
        }
        if (samplesDecoded < 1) {
            break;
        }

        if (appliedEffect != 3) // since it's already been written
            fwrite(intBuffer, 1, samplesDecoded * 4, fd);
    }

    closeWAV(fd);
}

extern "C" JNIEXPORT jint
Java_com_hmomeni_canto_activities_EditActivity_Effect(
        JNIEnv *__unused env,
        jobject __unused obj
) {
    return appliedEffect;
}

extern "C" JNIEXPORT void
Java_com_hmomeni_canto_activities_EditActivity_ApplyEffect(
        JNIEnv *__unused env,
        jobject __unused obj,
        jint effect
) {
    switch (effect) {
        default:
        case 0: // no effect

            player->setPitchShift(0);
            if (ed_isSinging)
                micPlayer->setPitchShift(0);

            appliedEffect = effect;
            break;
        case 1: // reverb
            if (reverb == nullptr) {
                reverb = new SuperpoweredReverb((unsigned int) sampleRate);
                reverb->enable(true);
            }
            reverb->setMix(0.8);

            player->setPitchShift(0);
            if (ed_isSinging)
                micPlayer->setPitchShift(0);

            appliedEffect = effect;
            break;
        case 2: // flanger
            if (flanger == nullptr) {
                flanger = new SuperpoweredFlanger((unsigned int) sampleRate);
                flanger->enable(true);
            }

            player->setPitchShift(0);
            if (ed_isSinging)
                micPlayer->setPitchShift(0);

            appliedEffect = effect;
            break;

        case 3:
            player->setPitchShift(5);
            if (ed_isSinging)
                micPlayer->setPitchShift(5);
            appliedEffect = effect;
            break;

    }
}