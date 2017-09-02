from enum import Enum
from road import Road
from util import absolute_path_for
import pygame

class GameStatus(Enum): 
    ON_MENU, PLAYING, PAUSED, EXITING = range(4)

class HighwayPatrol:
    def __init__(self, screen):
        self.status = GameStatus.PLAYING
        self.screen = screen
        self.road = Road(self.screen)
        
    def start(self):
        clock = pygame.time.Clock()
        car = pygame.image.load(absolute_path_for('/assets/sprites/cars/audi.png'))
        speed = 0
        
        while not self.status == GameStatus.EXITING:
            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    HighwayPatrol.close()
            
            if pygame.key.get_pressed()[pygame.K_UP] != 0:
                speed += 1/10 
            elif pygame.key.get_pressed()[pygame.K_DOWN] != 0:
                speed -= 1/10

            self.road.scroll(speed)
            
            pygame.display.update()
            clock.tick(60)
            
    @classmethod
    def close(cls):
        pygame.quit()
        quit()            

pygame.init()
pygame.display.set_caption('Highway Patrol')
screen = pygame.display.set_mode((500, 800))

HighwayPatrol(screen).start()