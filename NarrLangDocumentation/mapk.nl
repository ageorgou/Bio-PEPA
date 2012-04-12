/*
  A simple model of the MAPK signalling cascade
*/

//In general we start by defining constants, but in our case we don't want any

Compartments
(1, cytosol, 1.0, , 3, )


Components
(1, MAPKKK, "", , phosphorylated:FALSE, 1:(100, 100), (10, 100), (0,0), (0,0))
(2, MAPKK, "", , phosphorylated:FALSE, 1:(100, 100), (20, 100), (0,0), (0,0))
(3, MAPK, "", , phosphorylated:FALSE, 1:(100, 100), (20, 100), (0,0), (0,0))


Reactions
(1, phosphorylation, "MAPKKK phosphorylation", (fMA(0.1), 50), (1.0, 50))
(2, phosphorylation, "MAPKK phosphorylation", (fMA(0.1), 50), (1.0, 50))
(3, phosphorylation, "MAPK phosphorylation", (fMA(0.15), 50), (1.0, 50))


Narrative
Process "cascade"
(1, if MAPKKK is not phosphorylated then MAPKKK phosphorylates, "",1, ,)
(2, if MAPKKK is phosphorylated and MAPKK is not phosphorylated then MAPKKK phosphorylates MAPKK,"",2,,)
(3, if MAPKK is phosphorylated and MAPK is not phosphorylated then MAPKK phosphorylates MAPK,"",3,,)
