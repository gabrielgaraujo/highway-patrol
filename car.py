from util import absolute_path_for
import pygame
import time


class Car:
    image_path = absolute_path_for('/assets/sprites/cars/animated/police/')
    acceleration = 0.2
    car_image = pygame.image.load(image_path + "idle.png")

    car_animation = [
        pygame.image.load(image_path + '1.png'),
        pygame.image.load(image_path + '2.png'),
        pygame.image.load(image_path + '3.png')
    ]

    def __init__(self, surface):

        self.surface_width = surface.get_width()
        self.surface_height = surface.get_height()

        self.is_light_on = False
        self.speed = 0
        self.max_speed = 200

        self.odometer = 0
        self.speed_time_delta = time.time()

        self.rect = pygame.Rect((self.surface_width / 2.0) - (self.car_image.get_width() / 2.0),
                                500,
                                self.car_image.get_width(),
                                self.car_image.get_height())
        self.repeat = 0


    def turn_lights_on(self, screen):
        screen.blit(self.car_animation[0], (self.rect.left, self.rect.top))
        self.repeat += 1
        if self.repeat % 5 == 0:
            self.car_animation = self.car_animation[1:] + self.car_animation[:1]

    def turn_lights_off(self, screen):
        screen.blit(self.car_image, (self.rect.left, self.rect.top))

    def toggle_lights(self):
        self.is_light_on = not self.is_light_on

    def animate(self, screen):
        if self.is_light_on:
            self.turn_lights_off(screen)
        else:
            self.turn_lights_on(screen)

        if self.speed > 0:
            self.update_odometer()


    def turn_left(self):
        self.rect.left -= abs(self.speed / 1)

    def turn_right(self):
        self.rect.left += abs(self.speed / 1)

    def increase_speed(self):
        if self.speed < self.max_speed:
            self.speed += self.acceleration

    def reduce_speed(self):
        self.speed -= self.acceleration

    def update_odometer(self):

        actual_time = time.time()
        # in hours
        delta_time = (time.time() - self.speed_time_delta) / 3600
        self.speed_time_delta = actual_time

        self.odometer += (self.speed * delta_time) * 1000
