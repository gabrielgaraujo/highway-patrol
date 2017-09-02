from util import absolute_path_for
import pygame

class Road:
    def __init__(self, screen):
        texture = pygame.image.load(absolute_path_for('/assets/sprites/asphalt.jpg'))       
        self.screen = screen
        self.height = texture.get_height()
        self.texture = pygame.transform.scale(texture, (500, self.height))
        self.position = 0

    def display(self):
        self.screen.blit(self.texture, (0, self.position))
        self.screen.blit(self.texture, (0, self.position + self.height))

    def scroll(self, speed):        
        if speed > 0:
            self.position += abs(speed)
        elif speed < 0:    
            self.position -= abs(speed) 
        
        if self.position < -self.height:
            self.position = 0
        elif self.position + self.height > self.height:
            self.position = -self.height

        self.display()
        