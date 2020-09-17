import numpy as np
import scipy.optimize
import pandas as pd
import csv


def findV(ListR, TotalV, hashCode):
    n = len(ListR)
    GAMMA1 = 0.354183; GAMMA2 = 0.147688 # 39 clusters
    Alpha = [0] * n
    df = pd.read_csv('S:/USYD/Research/Decentralised Cruising/Taxi/ClusterData/cluster_alpha (39 clusters).csv', header=None)
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

    # Estimate from historical resource pattern. Use columns AFTER simulation warm-up
    historicalR = pd.read_csv('S:/USYD/Research/Decentralised Cruising/Taxi/Optimiser IO/historical_R (39 clusters).csv', header=None, usecols=[hashCode+12], squeeze=True).values
    newR = ListR + historicalR - np.multiply(np.multiply(Alpha, np.power(ListR, GAMMA1)), np.power(res.x, GAMMA2))
    newR[newR < 0] = 0
    np.savetxt('S:/USYD/Research/Decentralised Cruising/Taxi/Optimiser IO/input.csv', newR.reshape(1, newR.shape[0]), fmt='%.9f', delimiter=',')
    with open("S:/USYD/Research/Decentralised Cruising/Taxi/Resource and Expiration Results/Estimated R.csv", "a", newline='') as fp:
        wr = csv.writer(fp)
        wr.writerow(list(newR))
    return res.x


def findM(eigenVector):
    n = len(eigenVector)
    ub = []
    df = pd.read_csv('S:/USYD/Research/Decentralised Cruising/Taxi/ClusterData/cluster_min=50_max=500_alpha=-0.001_nb.csv', index_col='cluster_id')
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
    #bounds = scipy.optimize.Bounds([0]*n*n, [1]*n*n)

    res = scipy.optimize.minimize(f, x0, method='SLSQP', constraints=[eq_cons],
                                  options={'ftol': 1e-9, 'maxiter': 100 * len(x0)}, bounds=bounds)
    return res.x


javaDf = pd.read_csv('S:/USYD/Research/Decentralised Cruising/Taxi/Optimiser IO/input.csv', header=None)
R = javaDf.iloc[0, :].astype(float).tolist()
V = int(javaDf.iloc[1, 0])
hashCode = int(javaDf.iloc[2, 1])
    
np.savetxt('S:/USYD/Research/Decentralised Cruising/Taxi/Optimiser IO/output_{}.csv'.format(hashCode),
            findM(findV(R, V, hashCode)).reshape(len(R), len(R)), fmt='%.9f', delimiter=',')

with open('S:/USYD/Research/Decentralised Cruising/Taxi/Optimiser IO/output_{}.csv'.format(hashCode),'a') as fd:
    fd.write('hashcode,{}'.format(hashCode))
