from util import absolute_path_for
import pygame
import math
import random
import time


class Obstacle:

    def __init__(self, surface, street_speed):

        image_path = absolute_path_for('/assets/sprites/cars/')
        obstacles = ["audi", "car", "black_viper", "mini_truck"]
        filename = obstacles[math.ceil(random.uniform(0, len(obstacles)))-1]
        obstacle_image = pygame.image.load(image_path + filename + ".png")
        self.obstacle_image = obstacle_image


        self.surface_width = surface.get_width()
        self.surface_height = surface.get_height()
        self.y_pos = -(obstacle_image.get_height() + obstacle_image.get_height() * street_speed)

        x_origin = (self.surface_width / 2.0) - (obstacle_image.get_width() / 2.0)
        x_origin += random.uniform(-200, 200)
        self.x_origin = x_origin

        self.rect = pygame.Rect(x_origin,
                                self.y_pos,
                                obstacle_image.get_width(),
                                obstacle_image.get_height())
        self.repeat = 0
        self.start_time = time.time()
        self.random_start_time = math.ceil(random.uniform(0, 3))

    def animate(self, screen, speed):
        speed = abs(speed / 2)
        if speed > 0:
            self.y_pos += speed
        elif speed < 0:
            self.y_pos -= speed

        self.rect = pygame.Rect(self.x_origin,
                                self.y_pos,
                                self.obstacle_image.get_width(),
                                self.obstacle_image.get_height())

        actual_time = time.time()
        elapsed_since_origin = actual_time - self.start_time

        if elapsed_since_origin > self.random_start_time:
            screen.blit(self.obstacle_image,
                        (self.rect.left, self.rect.top))
