from util import absolute_path_for
import pygame
import math
import random
import time


class Obstacle:
    image_path = absolute_path_for('/assets/sprites/cars/')

    obstacle_image = pygame.image.load(image_path + "audi.png")

    def __init__(self, surface):


        self.surface_width = surface.get_width()
        self.surface_height = surface.get_height()
        self.y_pos = -self.obstacle_image.get_height()

        self.rect = pygame.Rect((self.surface_width / 2.0) - (self.obstacle_image.get_width() / 2.0),
                                self.y_pos,
                                self.obstacle_image.get_width(),
                                self.obstacle_image.get_height())
        self.repeat = 0
        self.start_time = time.time()
        # self.random_start_time = math.ceil(random.uniform(0, 10))
        self.random_start_time = 1


    def animate(self, screen, speed):

        if speed > 0:
            self.y_pos += abs(speed / 2)
        elif speed < 0:
            self.y_pos -= abs(speed / 2)

        self.rect = pygame.Rect((self.surface_width / 2.0) - (self.obstacle_image.get_width() / 2.0),
                                self.y_pos,
                                self.obstacle_image.get_width(),
                                self.obstacle_image.get_height())

        actual_time = time.time()
        elapsed_since_origin = actual_time - self.start_time

        if elapsed_since_origin > self.random_start_time:
            screen.blit(self.obstacle_image,
                        (self.rect.left, self.rect.top))















