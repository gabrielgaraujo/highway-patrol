from util import absolute_path_for
import pygame


def play_crash_sound():
    path = absolute_path_for('/assets/sounds/crash.ogg')
    pygame.mixer.music.load(path)
    if not pygame.mixer.music.get_busy():
        pygame.mixer.music.play()
