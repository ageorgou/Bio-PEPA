/*
  A model for the reaction:
  E + S <--> ES --> EP --> E + P
  
  Multiline comments can be added like this.
*/

//And single-line comments like this.

//The constants in the model
Constants
(k1,0.1)
(N,100)

/*
  Definition of a compartment: its id, name, dimension, units (here empty)
  number of dimensions (2 or 3) and the id if the compartment in which it
  is contained (if applicable, here empty)
*/

Compartments
(1, cytosol, 1.0, , 3, )


/*
  Components are the biochemical species. Their definitions have:
  - an ID
  - a name
  - a description (optional)
  - a list of sites, with the type of their state (bound, active, ...) and initial state (TRUE/FALSE)
  - a list of component-wide states, as above, but unnamed
  - a list of compartments in which the component can be found, along with the percentage that is
    initially in that compartment
  - synthesis and degradation rates (unused)
*/
Components
(1, E, "the enzyme", , bound:FALSE, 1:(100, 100), (N, 100), (0,0), (0,0))
(2, S, "the substrate, transformed into product", product:active:FALSE, bound:FALSE, 1:(100, 100), (1000, 100), (0,0), (0,0))

/*
  Reactions have an ID, a reaction type, a description (optional), a kinetic law
  and a reaction volume (unused)
*/
Reactions
(1, binding, "E-S binding, Ka (M-1 s-1)", (fMA(k1), 50), (1.0, 50))
(2, unbinding, "E-S unbinding, Koff (s-1)", (fMA(0.1), 50), (1.0, 50))
(3, activation, "S converted into P", (fMA(0.05), 50), (1.0, 50))
(4, unbinding, "E-P unbinding, Koff (s-1)", (fMA(0.1), 50), (1.0, 50))

Narrative

//Events can be grouped in processes for easier reading, but with no semantic significance
Process "E-S binding/unbinding"
/*
  An event has: an ID, a description (conditions and the actual change), description (optional),
  a reaction that realises it, and alternative/reverse events (optional)
  e.g. event 1 is realised by reaction 1- that's the last non-empty field in its definition
*/
  (1, if S is not bound and E is not bound and S.product is not active then S binds   E, "", 1, , )
  (2, if S is     bound and E is     bound and S.product is not active then S unbinds E, "", 2, reverse of 1, )

Process "Product formation"
  (3, if S is     bound and E is     bound and S.product is not active then E activates S on product, "", 3, , alternative to 2)
  (4, if S is     bound and E is     bound and S.product is     active then S unbinds E, "", 4, , )
