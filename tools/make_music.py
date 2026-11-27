#!/usr/bin/env python3
"""Render the game's background music to assets/sounds/music.ogg.

An original chiptune chase loop in A minor at 140 BPM: driving bass,
arpeggios, a lead melody and drums, synthesized from scratch with numpy.
The output loops seamlessly (note tails at the end wrap around to the start).

Usage:  python3 tools/make_music.py      (needs numpy and ffmpeg with libvorbis)
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

RATE = 44100
BPM = 140
STEP = 60 / BPM / 4  # one 16th note, in seconds
BAR = 16  # steps per bar

# Chords per bar. Section A: groove without the lead. Section B: with the lead.
CHORDS = {
    "Am": (45, [57, 60, 64]),  # (bass root, chord tones for the arpeggio)
    "F": (41, [53, 57, 60]),
    "C": (48, [55, 60, 64]),
    "G": (43, [55, 59, 62]),
    "E": (40, [56, 59, 64]),  # E major: G# leads back to A minor
}
SECTION_A = ["Am", "F", "C", "G", "Am", "F", "C", "E"]
SECTION_B = ["F", "G", "Am", "Am", "F", "G", "E", "E"]
SONG = SECTION_A + SECTION_B

# Lead melody for section B: one string per bar of "midi:length_in_16ths", "r" = rest.
LEAD_B = [
    "69:2 72:2 77:4 76:2 72:2 69:4",
    "71:2 74:2 79:4 77:2 74:2 71:4",
    "72:2 76:2 81:6 79:2 76:4",
    "r:4 81:2 79:2 76:2 74:2 72:4",
    "69:2 72:2 77:4 79:2 81:2 77:4",
    "79:2 77:2 74:4 71:2 74:2 79:4",
    "76:4 80:4 83:4 81:2 80:2",
    "76:8 r:8",
]


def freq(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def envelope(n, attack=0.005, decay=0.08, sustain=0.6, release=0.03):
    t = np.arange(n) / RATE
    env = np.where(t < attack, t / attack,
                   np.maximum(sustain, 1 - (1 - sustain) * (t - attack) / decay))
    rel = int(release * RATE)
    if rel and n > rel:
        env[-rel:] *= np.linspace(1, 0, rel)
    return env


def pulse(f, n, duty=0.5, vibrato=0.0):
    t = np.arange(n) / RATE
    if vibrato:
        # Gentle vibrato that fades in after the attack.
        depth = vibrato * np.clip((t - 0.1) / 0.15, 0, 1)
        f = f * 2 ** (depth * np.sin(2 * np.pi * 5.5 * t) / 12)
        phase = np.cumsum(f) / RATE
    else:
        phase = f * t
    return np.where(phase % 1 < duty, 1.0, -1.0)


def triangle(f, n):
    phase = f * np.arange(n) / RATE
    return 2 * np.abs(2 * (phase % 1) - 1) - 1


def kick():
    n = int(0.25 * RATE)
    t = np.arange(n) / RATE
    f = 45 + 110 * np.exp(-t / 0.03)
    return np.sin(2 * np.pi * np.cumsum(f) / RATE) * np.exp(-t / 0.09)


def snare(rng):
    n = int(0.18 * RATE)
    t = np.arange(n) / RATE
    noise = rng.uniform(-1, 1, n) * np.exp(-t / 0.05)
    tone = np.sin(2 * np.pi * 190 * t) * np.exp(-t / 0.04)
    return 0.7 * noise + 0.5 * tone


def hat(rng, length=0.03):
    n = int(0.12 * RATE)
    t = np.arange(n) / RATE
    noise = np.diff(rng.uniform(-1, 1, n + 1))  # crude high-pass
    return noise * np.exp(-t / length) * 0.5


def render():
    rng = np.random.default_rng(140)
    steps = len(SONG) * BAR
    length = int(steps * STEP * RATE)
    out = np.zeros(length + 2 * RATE)  # room for tails, wrapped at the end

    def add(sound, step, gain):
        start = int(step * STEP * RATE)
        out[start:start + len(sound)] += sound * gain

    def note(wave_fn, midi, step, length_steps, gain, **env):
        n = int(length_steps * STEP * RATE)
        add(wave_fn(freq(midi), n) * envelope(n, **env), step, gain)

    for bar, name in enumerate(SONG):
        root, tones = CHORDS[name]
        base = bar * BAR
        # Bass: driving eighths, jumping up an octave on the offbeats.
        for i, octave in enumerate([0, 0, 12, 0, 0, 12, 0, 12]):
            note(lambda f, n: pulse(f, n, duty=0.25), root + octave, base + i * 2, 2, 0.28,
                 decay=0.1, sustain=0.5)
        # Arpeggio: 16ths over the chord, an octave up.
        arp = tones + [tones[0] + 12]
        for i in range(BAR):
            note(triangle, arp[i % 4] + 12, base + i, 1, 0.10, decay=0.05, sustain=0.3)
        # Drums.
        for s in (0, 8, 10):
            add(kick(), base + s, 0.75)
        for s in (4, 12):
            add(snare(rng), base + s, 0.32)
        for s in range(0, BAR, 2):
            add(hat(rng, 0.06 if s % 4 == 2 else 0.02), base + s, 0.22)

    # Lead melody over section B.
    lead_start = len(SECTION_A) * BAR
    for bar, line in enumerate(LEAD_B):
        step = lead_start + bar * BAR
        for token in line.split():
            pitch, dur = token.split(":")
            dur = int(dur)
            if pitch != "r":
                note(lambda f, n: pulse(f, n, duty=0.5, vibrato=0.15), int(pitch), step, dur, 0.17,
                     decay=0.15, sustain=0.7, release=0.04)
            step += dur

    # Wrap the tails past the loop point back to the start so it loops cleanly.
    tail = out[length:]
    out = out[:length]
    out[:len(tail)] += tail

    # Soft-clip and normalize to -1 dBFS.
    out = np.tanh(out * 1.2)
    out *= 10 ** (-1 / 20) / np.max(np.abs(out))
    return out


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    target = os.path.join(root, "assets", "sounds", "music.ogg")
    samples = (render() * 32767).astype("<i2")
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "music.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(samples.tobytes())
        # Soften the raw square waves a little, then encode.
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav,
                        "-af", "lowpass=f=7000", "-c:a", "libvorbis", "-q:a", "4", target],
                       check=True)
    print(f"wrote {target} ({len(samples) / RATE:.1f} s)")


if __name__ == "__main__":
    main()
