GitHub repo https://github.com/m18a1/simpleHTTPserver

Run SimpleServer.jar and send HTTP requests to localhost:8888

I used Postman to send requests, you can get that from https://www.getpostman.com/

Bonuses:
1) I'm not familiar with fault tolerant deployment or topology. Since the Map I stored everything in isn't thread safe if multiple requests modifying the same partner all arrived from different clients that would result in erroneous data.
2) +HashMap is quick. -Not thread safe. -Works for small data.
3) Done. Sending HTTP GET request to /ad will return all ads.
4) Not implemented. Could continue to use the Map data sturcture but add a list of tuples as the value instead of just one Triple.