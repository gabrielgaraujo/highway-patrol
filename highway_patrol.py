from enum import Enum
from road import Road
from car import Car
import pygame
import sounds
import math


WHITE = (255, 255, 255)


class GameStatus(Enum):
    ON_MENU, PLAYING, PAUSED, EXITING = range(4)


class HighwayPatrol:

    def __init__(self, ascreen):

        self.font = pygame.font.SysFont('Monaco', 50, True, False)
        self.status = GameStatus.PLAYING
        self.road = Road(ascreen)

        surface: pygame.Surface = pygame.display.get_surface()
        self.surface_width = surface.get_width()
        self.surface_height = surface.get_height()

        self.car = Car(surface)

    def start(self):
        clock = pygame.time.Clock()

        while not self.status == GameStatus.EXITING:
            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    HighwayPatrol.close()

                if event.type == pygame.MOUSEBUTTONUP:
                    self.car.toggle_lights()
            
            if pygame.key.get_pressed()[pygame.K_UP] != 0:
                self.car.increase_speed()
            elif pygame.key.get_pressed()[pygame.K_DOWN] != 0:
                self.car.reduce_speed()

            self.road.scroll(self.car.speed)

            crashed = False
            if pygame.key.get_pressed()[pygame.K_LEFT] != 0:
                self.car.turn_left()
                if self.car.rect.left <= 0:
                    self.car.rect.left = 1
                    crashed = True
            elif pygame.key.get_pressed()[pygame.K_RIGHT] != 0:
                self.car.turn_right()
                if self.car.rect.right >= self.surface_width:
                    self.car.rect.right = self.surface_width - 1
                    crashed = True

            self.car.animate(screen)

            self.display_car_speed(self.car.speed)

            if crashed:
                sounds.play_crash_sound()

            pygame.display.update()
            clock.tick(60)
            
    @classmethod
    def close(cls):
        pygame.quit()
        quit()

    def display_car_speed(self, speed):

        speed = math.ceil(speed)
        text_surface: pygame.Surface = self.font.render("{:.0f} km/h".format(speed),
                                                        True,
                                                        WHITE)

        screen.blit(text_surface,
                    [20,
                     (text_surface.get_height() / 2)])


pygame.init()
pygame.display.set_caption('Highway Patrol')
screen = pygame.display.set_mode((500, 800))

HighwayPatrol(screen).start()
