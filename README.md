A repository for building [`scala-web-framework`](https://github.com/arturaz/scala-web-framework) as Maven libraries.

You can also use this as a template for your own project.

## Usage

### Initialization

```
git submodule update --init
./mill __.compile
```

### Running shared code tests

```
./mill appShared.tests.testForked
```

### Running server in continuous compilation mode

```
./mill -w appServer.runBackground
```

After running you should be able to access http://localhost:3005/hello

Note: the default server expects to have OpenTelemetry collector running on `localhost:4317`. If that isn't running
it will occasionally log errors to the console.