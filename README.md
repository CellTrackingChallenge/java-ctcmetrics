Welcome
-------
This is a repository with Java source codes of tools related to the [Cell Tracking Challenge](http://www.celltrackingchallenge.net), and to the quantitative evaluation of biomedical tracking in general.
In particular, one can find here:

* Technical (developer-oriented) tracking and segmentation measures: TRA, SEG, DET
* Biological (user-oriented) measures: CT, TF, BC(i), CCA
* Dataset quality measures: SNR, CR, Hetb, Heti, Res, Sha, Den, Cha, Ove, Mit
* Tracking accuracy evaluation with general [Acyclic Oriented Graphs Measure (AOGM)](http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0144959)

The measures were used in the paper [An objective comparison of cell-tracking algorithms](http://dx.doi.org/10.1038/nmeth.4473) and are,
together with the detection accuracy measure DET, complementing the measures used in the [Challenge](http://www.celltrackingchallenge.net).

The ideas, that are implemented in the tools, are product of a collective collaboration between [CIMA](http://www.cima.es),
[CBIA](http://cbia.fi.muni.cz), [Erasmus MC](https://www.erasmusmc.nl/oic/?lang=en), [UC3M](https://www.uc3m.es),
[CSBD](http://www.csbdresden.de/) and [MPI-CBG](http://mpi-cbg.de) groups.

The tools were developed and the page is maintained by [Vladimír Ulman](http://www.fi.muni.cz/~xulman/).
The SEG, TRA/AOGM and DET measures were originally developed in C++ by [Martin Maška](http://cbia.fi.muni.cz/).

Example
-------
[Here, one can find example of the API calls](https://github.com/CellTrackingChallenge/measures/blob/e4ac070475b7c50d0d89aecf3c4e74396437eda4/src/test/java/net/celltrackingchallenge/measures/TestMeasures.java#L74) to have the seven (TRA.... CCA) measures calculated over your data.

The data needs to be, however, organized in a special way [(see Naming and Content conventions)](http://public.celltrackingchallenge.net/documents/Naming%20and%20file%20content%20conventions.pdf):

![Example of data layout](src/test/java/net/celltrackingchallenge/measures/test_data.png)
