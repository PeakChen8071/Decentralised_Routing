import numpy as np
import scipy.optimize
import pandas as pd


def findV(ListR, TotalV):
    n = len(ListR)
    # GAMMA1 = 0.304938; GAMMA2 = 0.224892
    GAMMA1 = 0.227927; GAMMA2 = 0.233829
    Alpha = [0] * n
    df = pd.read_csv('S:\\USYD\\Research\\Decentralised Cruising\\Taxi\\ClusterData\\cluster_alpha (23 clusters).csv', header=None)
    for i in df.index:
        Alpha[df.iloc[i, 0]] = df.iloc[i, 1]
        
    def f(x):
        return -np.dot(np.multiply(Alpha, np.power(ListR, GAMMA1)), np.power(x, GAMMA2))

    eq_cons = {'type': 'eq',
               'fun': lambda x: np.array([sum(x) - TotalV])}

    x0 = [TotalV / n] * n

    bounds = scipy.optimize.Bounds([0] * n, [TotalV] * n)

    res = scipy.optimize.minimize(f, x0, method='SLSQP', constraints=[eq_cons],
                                  options={'ftol': 1e-9, 'maxiter': 100 * len(x0)}, bounds=bounds)
    return res.x


def findM(eigenVector):
    n = len(eigenVector)
    ub = []
    df = pd.read_csv('S:/USYD/Research/Decentralised Cruising/Taxi/ClusterData/cluster_min=70_max=10000_alpha=-0.001_nb.csv', index_col='cluster_id')
    for i in df.index.sort_values():
        adjacency = [0] * n
        for j in df.loc[i].str.split()[0]:
            adjacency[i] = 1
            adjacency[int(j)] = 1
        ub += adjacency
    
    def f(x):
        return np.linalg.norm(x.reshape(n, n).dot(eigenVector) - eigenVector)

    eq_cons = {'type': 'eq',
               'fun': lambda x: np.array([x.reshape(n, n)[:, i].sum() - 1 for i in range(n)])}

    x0 = [1 / n] * n * n

    bounds = scipy.optimize.Bounds([0]*n*n, ub)

    res = scipy.optimize.minimize(f, x0, method='SLSQP', constraints=[eq_cons],
                                  options={'ftol': 1e-9, 'maxiter': 100 * len(x0)}, bounds=bounds)
    return res.x


javaDf = pd.read_csv('S:\\USYD\Research\\Decentralised Cruising\\Taxi\\Optimiser IO\\input.csv', header=None)
R = javaDf.iloc[0, :].astype(int).tolist()
V = int(javaDf.iloc[1, 0])
hashCode = int(javaDf.iloc[2, 1])
    
np.savetxt('S:\\USYD\\Research\\Decentralised Cruising\\Taxi\\Optimiser IO\\output_{}.csv'.format(hashCode),
            findM(findV(R, V)).reshape(len(R), len(R)), delimiter=',')

with open('S:\\USYD\\Research\\Decentralised Cruising\\Taxi\\Optimiser IO\\output_{}.csv'.format(hashCode),'a') as fd:
    fd.write('hashcode,{}'.format(hashCode))
