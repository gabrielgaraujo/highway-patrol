from enum import Enum
from road import Road
from car import Car
import pygame
import sounds
from hud import HUD

from obstacle import Obstacle


class GameStatus(Enum):
    ON_MENU, PLAYING, PAUSED, EXITING = range(4)


class HighwayPatrol:

    def __init__(self, asurface):

        self.crashed = False

        self.hud = HUD(asurface)

        self.status = GameStatus.PLAYING
        self.road = Road(asurface)

        self.surface = asurface
        self.surface_width = surface.get_width()
        self.surface_height = surface.get_height()

        self.car = Car(surface)
        self.obstacles = []
        self.adding_obstacle = False

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


            if pygame.key.get_pressed()[pygame.K_LEFT] != 0:
                self.car.turn_left()
                if self.car.rect.left <= 0:
                    self.car.rect.left = 1
            elif pygame.key.get_pressed()[pygame.K_RIGHT] != 0:
                self.car.turn_right()
                if self.car.rect.right >= self.surface_width:
                    self.car.rect.right = self.surface_width - 1

            if not self.crashed:
                self.road.scroll(self.car.speed)
                self.car.animate(surface)

                self.add_random_obstacle(surface)

                for obstacle in self.obstacles:
                    obstacle.animate(surface, self.car.speed)
                    collided = self.car.rect.colliderect(obstacle.rect)

                    if collided:
                        self.crashed = True
                        sounds.play_crash_sound()

                self.hud.display_car_speed(self.car.speed)
                self.hud.display_car_odometer(self.car.odometer)

            pygame.display.update()
            clock.tick(60)
            
    @classmethod
    def close(cls):
        pygame.quit()
        quit()

    def add_random_obstacle(self, surface: pygame.Surface):

        if self.visible_obstacles == 0 and not self.adding_obstacle:
            self.adding_obstacle = True
            self.add_obstacle(surface)


    @property
    def visible_obstacles(self):
        count = 0
        for obstacle in self.obstacles:
            if self.surface.get_rect().contains(obstacle.rect):
                count += 1
        if count > 0:
            self.adding_obstacle = False
        return count

    def add_obstacle(self, surface):
        self.adding_obstacle = True
        obstacle = Obstacle(surface, self.car.speed)
        self.obstacles.append(obstacle)





pygame.init()
pygame.display.set_caption('Highway Patrol')
surface = pygame.display.set_mode((500, 800))

HighwayPatrol(surface).start()
