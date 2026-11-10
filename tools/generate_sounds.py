"""Generate the three bundled completion chimes as small PCM WAV files."""

from __future__ import annotations

import math
import struct
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app" / "src" / "main" / "res" / "raw"
OUT.mkdir(parents=True, exist_ok=True)

SAMPLE_RATE = 44_100


def envelope(t: float, duration: float, attack: float = 0.02, release: float = 0.35) -> float:
    if t < attack:
        return t / attack
    if t > duration - release:
        return max(0.0, (duration - t) / release)
    return 1.0


def tone(t: float, frequency: float, harmonics: list[tuple[float, float]]) -> float:
    value = 0.0
    for multiple, amplitude in harmonics:
        value += amplitude * math.sin(2 * math.pi * frequency * multiple * t)
    return value


def write_wav(name: str, duration: float, notes: list[tuple[float, float, float, list[tuple[float, float]]]]) -> None:
    samples: list[int] = []
    total = int(SAMPLE_RATE * duration)
    for index in range(total):
        t = index / SAMPLE_RATE
        value = 0.0
        for start, note_duration, frequency, harmonics in notes:
            if start <= t < start + note_duration:
                local = t - start
                value += tone(local, frequency, harmonics) * envelope(local, note_duration) * 0.30
        value = max(-1.0, min(1.0, value))
        samples.append(int(value * 32767))
    path = OUT / name
    with wave.open(str(path), "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(SAMPLE_RATE)
        handle.writeframes(b"".join(struct.pack("<h", sample) for sample in samples))
    print(f"wrote {path} ({path.stat().st_size} bytes)")


def main() -> None:
    # Soft two-note notification: C5 then E5, mostly fundamental.
    write_wav(
        "gentle_chime.wav",
        1.8,
        [
            (0.00, 0.85, 523.25, [(1.0, 1.0), (2.0, 0.08)]),
            (0.45, 0.95, 659.25, [(1.0, 0.9), (2.0, 0.06)]),
        ],
    )
    # Brighter three-note chime: E5, G5, C6 with a little harmonic sparkle.
    write_wav(
        "chime.wav",
        2.0,
        [
            (0.00, 0.70, 659.25, [(1.0, 1.0), (2.0, 0.22), (3.0, 0.08)]),
            (0.32, 0.75, 783.99, [(1.0, 0.9), (2.0, 0.20), (3.0, 0.07)]),
            (0.64, 1.10, 1046.50, [(1.0, 0.85), (2.0, 0.18), (3.0, 0.06)]),
        ],
    )
    # Deeper alarm bell: A4 fundamental with a long exponential decay.
    write_wav(
        "bell.wav",
        2.8,
        [
            (0.00, 2.75, 440.00, [(1.0, 1.0), (2.0, 0.45), (3.0, 0.22), (4.0, 0.10), (0.5, 0.25)]),
        ],
    )


if __name__ == "__main__":
    main()
