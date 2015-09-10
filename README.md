# lolproxy
A proxy server which enforce the respect of the Riot's http429 Policy (cf https://developer.riotgames.com/docs/rate-limiting)
Created by the creator of http://www.leagueofgraphs.com.

# Goal
The goal of this software is to provide an easy way to run requests against Riot's API servers, without taking the risk to be banned for overloading their APIs.
This software acts as a proxy, which will limit your request rate against Riot's APIs. So, in all your calls, add this proxy (127.0.0.1:23830) to your calls, and you should be alright.
Also, this helps a lot to enforce the respect of Riot's policy, in case of you are running multiple softwares which are all calling the API.


# How this works
The proxy enforces two separate limitations:
- The maximum amount of requests per second (RPS) you can make (by default: 220). 
- When Riot returns you an http429, it will take it into account, and even over protect Riot: it will add 1000ms to the RetryAfter header returned by Riot, and it will wait at least 3s before accepting requests again. Also, it always act as if the http429 was "user".

When one of the two limitations has been reached, the proxy will not forward the http requests it receives to Riot's servers anymore, and will return you an http429 instead. 
Thus, even if you hit the proxy as hard as you can, Riot's server will be protected.


# Known issues
Requests are run in parallel. 
Thus, if a request has already been started and sent to Riot server when the proxy received an http429, all the already started requests will also return http429.
You can limit the number of concurrent requests: to do so, modify the number of threads the proxy uses. There will not be more parallel requests than number of threads.


# About HTTPS:
Requests have to be run in HTTP, and then the proxy will upgrade the request and run it in HTTPS.
YourApp <-- HTTP --> Proxy <-- HTTPS --> Riot

In my particular case, the proxy runs on the same machine as the other applications of my website. So having the YourApp <--> Proxy link in HTTP is not a problem.
Also, I didn't want to bother with the complexity of MITM for HTTPS (and certificate generation and all that stuff)

So, I strongly suggest that you run this proxy on the same machine as your applications.


# User restrictions
There is no user restrictions / filtering at all implemented in this proxy. Thus, I would suggest you to make sure that the proxy port is closed for the outside world.
