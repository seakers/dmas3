import tools.vis_tools as vt

problem = 'dmas3-template'

power = vt.load_power_data(problem)
scores = vt.load_score_data(problem)
vt.plot_results(power,scores)

x = 1