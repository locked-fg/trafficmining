# Running the GUI #
Just download the latest release of our code (currently: `MARiO_v2.zip` [download here](http://trafficmining.googlecode.com/files/MARiO_v2.zip)). Extract it into any directory and execute the `Trafficmining.jar` - that should do the job.

## Running an Algorithm ##
  1. Load a Graph: `File > Load Graph` or `Ctrl + O` onto your keyboard, then navigate to the directory containing the osm file and select it. Afterwards, the map should be zoomed onto the graphs data painted in red lines.
  1. Edit the Source and Destination Nodes by clicking onto `edit`. The first node you click is automatically set as source and the last node you select as destination node. The nodes are displayed as big blue arrows at the map. If you want to delete a special node, select it at the `Waypoints field` and press `Del` onto your keyboard; all nodes will be resetted if you click `clear`.
  1. Address search is furthermore possible. Click onto `address` and enter in the following window parts or the full streetname. Matching nodes are displayed in the field below and if clicked, marked as a blue arrow. When you have selected the right node, click `OK` and the node will be added to the `Waypoints field`.
  1. Choose the algorithm you want to execute from the `Algorithm dropdown box`.
  1. If your chosen algorithm has some attributes, which had to be configured, they can be accessed by clicking onto the `...` below the dropdown. In the `Configure window` you have the possibility to access and change the values of all attributes.
  1. Click the `Execute` button and wait until the algorithm has finished.
  1. All found routes are presented at the `Routes field`. The first tab shows the routes in a list and the second in a hierarchical form. All routes can be selected individual and are shown at the map as blue lines.

## Converting a pbf to an osm with additional SRTM data ##
  1. Import an pbf: `File > Import PBF`.
  1. Click `...` by `PBF Import` and navigate to the directory containing the pbf file.
  1. Click the button by `OSM Output` and navigate to the directory in which the osm file should be created.
  1. (Optional) Check the box of `SRTM directory` and choose the directory, where the the SRTM files are stored.
  1. Click onto the map and drag the mouse to select a region of interest. The latitude and longitude of your ROI are shown at the left. The intersection between the ROI and the borders of the pbf are shown as opaque red.
  1. If everything is correct, click onto `run` and wait until the process has finished. (This will take quite a while so get yourself one, two or more coffees ;)

## Algorithms ##
### Preimplemented algorithms ###
The project is delivered with five pre implemented algorithms. There are three demo algorithms and two usecase algorithms: [Skyline Algorithm](http://ieeexplore.ieee.org/xpl/freeabs_all.jsp?arnumber=5447845) and Dijkstra.
  * Demo: These three demonstrate the different use of the simplex (1D, 2D and 3D) and serve as examples how to extend the algorithm class and create an useful BeanInfo class.
  * The Dijkstra algorithm has one attribute: fastest or shortest path.
  * The Skyline algorithm has four attributes of which up to three can be selected (distance, height, time, traffic\_lights). When the Execute button is pressed, an embedding cache file is created. Depending on the graph, this can take quite some time. The embedding (name and path can be also configured as an attribute) is default saved in the directory where you executed the jar as `embedding.cache.txt`. This embedding is reused the next time you run the SkyLine algorithm.

'''Attention:''' After loading another graph and before starting the SkyLine algorithm, you must delete the embedding file or change its name manually! Otherwise you use a wrong embedding file.


## Implementing own algorithm ##
It is quite easy to do this. Just create any class and extend the class `de.lmu.ifi.dbs.paros.algorithms.Algorithm`.

You have to:
  * Overwrite the methods: `getName()`, `run()`, `getResult()` and `getStatistics()`
  * Implement any methods your algorithm needs to run correctly
  * (Optional but recommend) Create a BeanInfo for your class to provide an more comfortable way for selecting the attributes of your algorithm inside the Trafficmining.
  * create a jar of your compiled algorithm and put it into the plugins directory, restart Trafficmining and select it in the `Algorithm dropdown box`. You can put as many algorithms as you want into one single jar.
  * If you need help or examples how to implement your algorithm, don't by shy and have a look at the source code classes of the Demo algorithms found at `de.lmu.ifi.dbs.paros.algorithms`.