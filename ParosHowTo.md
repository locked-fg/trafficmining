# How To use TrafficMining v1 (PAROS) #
Paros is a Java based, open source program that allows an easy integration of route search algorithms (like [Dijkstra](http://en.wikipedia.org/wiki/Dijkstra%27s_algorithm)). Using paros, you can easily write new algorithms, test them on real daat and visualize the results without having to deal with GUI programming.

When I presented the demo at [SIGMOD 2010](http://www.sigmod2010.org/index.shtml), quite a lot of people asked me, if the demo would be also available for download. - After that, we wanted to clean the code, add some documentation and all that - but never really found the time to do that. Thus I decided to just put it online.

'''The good news:''' even without any introduction and documentation, some students already implemented parts of their bachelor- and diploma thesis using PAROS without any introduction from us. So it's really not that hard to get into the code!

The purpose of PAROS is:
  * for research: test and graphically verify your graph algorithms on real data from [OpenStreetMap](http://www.openstreetmap.org)
  * for research & teaching: a framework you can give to students which should get in touch with graph search but should not be delayed by GUI programming
  * for everyone else, if you just want to play around with route search :-)

# Why should I use PAROS #
In this demo, we showed how the concept of skyline queries can be successfully adapted to routing problems considering multiple road attributes. In particular, we demonstrate how to compute several pareto-optimal paths in road networks.

But for you as a developer and researcher, PAROS provides:
  * A pluggable architecture for developing your own route search algorithms
  * A GUI that visualizes your results without having to do some GUI coding!
  * Easy integration of Web Map Services if you wish to do so
  * Easy access to a huge repostiory of data worldwide by using [OpenStreetMap](http://www.openstreetmap.com) data (ever got a reject because you used "unrealistic, artificial" or "too small" data sets?)
  * If you don't want/need a GUI (for example just verify research results), you do not NEED the gui. You can integrate the code into your code base rather easy. All Algorithms can be started from your code as well.


# Brief Manual #
## Getting and Converting Data (optional) ##
You can just use the data provided in the [download section](http://www.dbs.ifi.lmu.de/cms/Project_PAROS#Downloads) under "Sample Graphs" (please [cite our paper](Citation.md) in that case, as we also used it in the demo). Just download and unpack the files. You should have a _nodes.txt_ and a _ways.txt_ after unpacking.

## Running the GUI ##
Just run the main Method in **experimentalcode.franz.Demo**. Don't forget to start the GUI with some RAM (-Xmx1000m for example) as we're reading the street graph later.

In [NetBeans](http://www.netbeans.org/), you can just press the run-Button, everything should be preconfigured.


### Running an Algorithm ###
  1. Load a Graph: _File > Load Graph_, then navigate to a directory containing the nodes.txt and ways.txt, select and load both. Possible errors are displayed in the console. After loading, the graph should be painted in red lines on the map.
  1. Set Source and Destination Nodes by first clicking the according Set-button and then click on the node on the map. A blue balloon marks marks the nodes.
  1. Choose the algorithm that you want to execute from the dropdown
  1. Choose the according parameters. Depending on the algorithm you can choose 1,2 or 3 attributes at the same time.
  1. Click the search button to star the algorithm.
  1. After the algorithm has terminated, the resulting paths are shown in the gui and you're done.


### The Skyline Algorithm ###
The [Skyline Algorithm](http://ieeexplore.ieee.org/xpl/freeabs_all.jsp?arnumber=5447845) can be chosen by selecting **experimentalcode.franz.OSMSkyline** and afterwards selecting up to 3 attributes.
When the Search button is pressed, an embedding is created (watch the console output for details). Depending on the graph, this can take quite some time.
The embedding file (called **embedding.txt**) is saved in the directory where yuo executed the jar. This embedding is reused the next time you run the SkyLine algorithm.

'''Attention:''' After loading another graph and before starting the skyline algorithm, you must delete the embedding file manually! Otherwise you use a wrong embedding file.


## Implementing own Algorithms ##
Implementing your own algorithm is rather easy! Just create a class and extend **experimentalcode.franz.Algorithm**. All you need to do is
  * implementing is the run()-method,
  * registering the algorithm in the GUI by calling **Demo d = new Demo(); d.addAlgorithm(YourDemoExtendingAlgorithm.class);** (see **experimentalcode.franz.Demo#main()** for an example how to register your algorithm).

The classes **DemoAlgorithm1D, DemoAlgorithm2D, DemoAlgorithm3D** implement toy-algorithms that reutrn 1D, 2D and 3D results which you can use as templates.


# Support / Mailinglists #
I am currently doing my PhD, please understand that I cannot give extensive support for the program as I am heavily involved in different programs. But be assured that I am still working on PAROS and I'll try to help if you have questions!

The following channels are provided for help and updates:
  * [PAROS-user mailinglist](https://tools.rz.ifi.lmu.de/mailman/listinfo/paros-users) for discussions and getting help.
  * [My Blog](http://frickelblog.wordpress.com) - If there's something new, I'll also post it there



# Bibtex #
If you use PAROS for your research, we would be really very happy to hear about it! Also please honour our work and cite us, if you are using it:

<pre>
@inproceedings{1807318,<br>
author = {Graf, Franz and Kriegel, Hans-Peter and Renz, Matthias and Schubert, Matthias},<br>
title = {PAROS: pareto optimal route selection},<br>
booktitle = {SIGMOD '10: Proceedings of the 2010 international conference on Management of data},<br>
year = {2010},<br>
isbn = {978-1-4503-0032-2},<br>
pages = {1199--1202},<br>
location = {Indianapolis, Indiana, USA},<br>
doi = {http://doi.acm.org/10.1145/1807167.1807318},<br>
publisher = {ACM},<br>
address = {New York, NY, USA}<br>
}<br>
</pre>