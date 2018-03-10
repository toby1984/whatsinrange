# whatsinrange
A toy project that extracts public transport stations from OpenStreetMap XML data, stores them in a PostgreSQL + PostGIS database ,then uses PhantomJS for scraping public transport travel times and finally uses the Google Maps API to render a heatmap with colors indicating travel times.

This map shows all locations that can be reached by public transport within 60 minutes from Hamburg central station.

![screenshot](https://github.com/toby1984/whatsinrange/blob/master/screenshot.png?raw=true)

The markers show places where there's a public transport stop according to OSM but I was unable to scrape the travel time from the HVV website.

I deployed an interactive version here: http://www.code-sourcery.de/whatsinrange.html

## Implementation details

- XML parsing uses a StaX parser (the data export from OSM was 92x48 km = 1.1 GB so I figured I needed something fast). Parsing the XML and importing it into a PostgreSQL database took ~10 seconds on my workstation (i7 6700k,SSD,24GB RAM)
- I used Selenium with the PhantomJS webdriver, mostly because I've already used it in another project. PhantomJS is unmaintained now so for anything serious I definitely use something different
- I used PostGIS for convenience (to implement nearest-neighbour queries) but probably could've gotten away with just building a KD-Tree and using Euclidian distances since distortion due to the spherical nature of earth can be neglected for such a small section of the map... but I never used PostGIS (or worked with geographical data) before and was curious...
- I used Google Maps because I couldn't find another easy way of rendering my data as an overlay over a zoomable map. I'm actually kind of disappointed with the Google Maps way of rendering heatmaps, I would've expected that the rendering is independent from the zoom-level and would automatically interpolate missing values (or at least provide a way to do so yourself, I understand that interpolation doesn't make sense for all kinds of data). If I wanted to spend more time on this project I would write my own renderer that does this... if you know a way how I can do this without having to write code, please get in touch with me :)

## Lessions learned

1. It always takes longer than you think

What started out as a "shouldn't take longer than an afternoon" project actually took me more like 4-5 afternoons...

2. It's not as easy as you think

My first mistake was thinking that public transport locations in the OpenStreetMap XML would be tagged consistenly. After playing around some time with the XML I realized that this is obviously not true so it took me several iterations to extract all (or maybe almost all) locations correctly.
My second mistake was thinking that the names assigned to public transport locations by the OpenStreetMap people would match the spelling/names used by the HVV (Hamburg public transport organization) whose website I used to retrieve the travel times. After spending a lot of time tweaking my website scraper I was finally able to get the number of locations where I couldn't get a travel time for down to an acceptable level:

      n   |     node_type      | scraping |     ratio %      
    ------+--------------------+----------+------------------
     3494 | bus_stop           | got data | 89.7047496790757
      401 | bus_stop           | no data  | 10.2952503209243
       85 | subway_station     | got data | 96.5909090909091
        3 | subway_station     | no data  | 3.40909090909091
       45 | light_rail_station | got data | 97.8260869565217
        1 | light_rail_station | no data  | 2.17391304347826
        6 | train_station      | got data |              100

Scraping failures are caused by ambiguous (or sometimes plain wrong) names in the OpenStreetMap data. I actually added some code so I could resolve these manually but doing this for 244 places is rather tedious so I didn't really try.
If I wanted to improve on this I would probably import *all* locations from the OSM dump into the database (and not just the public transport stops) and whenever scraping fails I would do a nearest-neighbour search for the closest building that's tagged with an address and use the address for scraping instead.
