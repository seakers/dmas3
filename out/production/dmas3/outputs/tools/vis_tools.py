import numpy as np
import matplotlib.pyplot as plt
import csv


class Power:
    def __init__(self):
        self.time = []
        self.epoch = []
        self.data = []
        self.sat_names = []
        self.length = 0


def load_power_data(problemStatement):
    with open('../' + problemStatement + '/power.csv') as csvfile:
        powerCSV = csv.reader(csvfile)
        power = Power()
        L = 0
        for row in powerCSV:
            l = len(row)
            if(L == 0):
                power.sat_names.append(row[2:l])
                L += 1
                continue
            power.time.append(row[0])
            power.epoch.append(row[1])
            power.data.append(row[2:(3+len(power.sat_names))])
            L += 1

        power.length = L-1
        return power


def plot_power(power):
    t = power.epoch
    for sat in range(len(power.sat_names)+1):
        data = [0.0] * len(t)
        for i in range(len(t)):
            data[i] = float(power.data[i][sat])
        plt.plot(t, data)
    plt.grid(True)
    plt.show()


class Scores:
    def __init__(self):
        self.tasks = {}

    def addTask(self, task):
        self.tasks[task.name] = task

    def addSubtask(self, subtask):
        task = self.tasks[subtask.parent_task]
        task.addSubtask(subtask)

class Task:
    def __init__(self, name="nil", max_score=-1.0,
                 score=-1.0, completion=-1.0):
        self.name = name
        self.max_score = max_score
        self.score = score
        self.completion = completion
        self.subtasks = []

    def addSubtask(self, subtask):
        self.subtasks.append(subtask)

class Subtask:
    def __init__(self, parent_task="nil", freq=-1.0, n_looks=-1, score=-1.0,
                 winner="nil", lat_location=-1.0, lon_location=-1.0,
                 lat_measurement=-1.0, lon_measurement=-1.0, time_measurement="nil",
                 completion=False):
        self.parent_task= parent_task
        self.freq = freq
        self.n_looks = n_looks
        self.score = score
        self.winner = winner
        self.lat_location = lat_location
        self.lon_location = lon_location
        self.lat_measurement = lat_measurement
        self.lon_measurement = lon_measurement
        self.time_measurement = time_measurement
        self.completion = completion


def load_score_data(problemStatement):
    scores = Scores()
    with open('../' + problemStatement + '/taskScores.csv') as csvfile:
        tasks_csv = csv.reader(csvfile)
        i = 0
        for row in tasks_csv:
            if i == 0:
                i += 1
                continue
            task = Task(str(row[0]), float(row[1]), float(row[2]), float(row[3]))
            scores.addTask(task)

    with open('../' + problemStatement + '/subtaskScores.csv') as csvfile:
        subtasks_csv = csv.reader(csvfile)
        i = 0
        for row in subtasks_csv:
            if i == 0:
                i += 1
                continue
            subtask = Subtask(str(row[0]), float(row[1]), int(row[2]), float(row[3]), str(row[4]),
                              float(row[5]), float(row[6]), float(row[7]), float(row[8]), str(row[9]),
                              bool(row[10]))
            scores.addSubtask(subtask)
    return scores

def plot_scores(scores):
    x = 1


def plot_results(power,scores):
    plot_power(power)
    plot_scores(scores)