import os

def absolute_path_for(relative_path):
    return os.path.dirname(os.path.abspath(__file__)) + relative_path