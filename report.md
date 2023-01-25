## To test the project

To test this project, you can start the cluster using docker-compose:

```bash
docker-compose up --build
```

After waiting a few seconds, you can run

```bash
curl -X "PUT" localhost:8001 -d "https://www.tum.de/"
```

to create the shortened version of the URL (which should look something like `localhost:8001/LTExNjgwODU2NTg=`).

Then you can call

```bash
curl -L localhost:8001/LTExNjgwODU2NTg= -v
```

and you should see one of the lines containing

```txt
Location: https://www.tum.de/
```

Or you can also navigate to http://localhost:8001/LTExNjgwODU2NTg= in your browser, and verify that your browser automatically redirects you.
