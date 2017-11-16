import math
import pygame

WHITE = (255, 255, 255)

class HUD:

    def __init__(self, surface):
        self.surface = surface
        self.font = pygame.font.SysFont('Monaco', 50, True, False)


    def display_car_speed(self, speed):
        speed = math.ceil(speed)
        text_surface: pygame.Surface = self.font.render("{:.0f} km/h".format(speed),
                                                        True,
                                                        WHITE)

        self.surface.blit(text_surface,
                    [20,
                     (text_surface.get_height() / 2)])


    def display_car_odometer(self, odometer):
        odometer = math.ceil(odometer)
        text_surface: pygame.Surface = self.font.render("{:.0f} m".format(odometer),
                                                        True,
                                                        WHITE)

        self.surface.blit(text_surface,
                    [self.surface.get_width() - text_surface.get_width() - 20,
                     (text_surface.get_height() / 2)])
