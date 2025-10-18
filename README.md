# Specialization


## Setup

### Step 1 - Import
- Basically create a new project, import the gradle project into IntelliJ, try building. It'll fail.
- Install suggested plugins etc.
- Wait for all the imports to finish
- Try building the project using the gradle tab, it will fail saying "username cannot be null".

### Step 2 - Gradle Properties

In the project root, create "server.properties" (this is already added to git.ignore). **Do not share the contents of this file.**

Your gradle.properties should look like this:
```
gpr.user=ExampleUsername
gpr.key=123456789
export.path=C:\\path\\to\\fun_game_dev_stuff\\specialization_dev\\default-server-config\\plugins
```

For ```gpr.user```, replace ```ExampleUsername``` with your GitHub account name.

For ```gpr.key```, we need to generate a github access token:
  - Go to github -> settings -> developer settings -> Personal access tokens -> tokens (classic) -> generate new token (classic)
  - Ensure you have write:packages (this will automatically check other things such as "read:packages")
  - Create it, and copy the token key, and put that as the ```gpr.key```

For ```export.path```, this is where the plugin will be exported to.

### Step 3 - 

Jdk 23 not supported

To fix this, ensure you have the appropriate JDK installed (as of this readme, it's 23).
Go to settings -> Build, Execution, Deployment
Build Tool -> Gradle -> Gradle JVM  **[Set this to 23]**

Then run the Gradle Build again, things should start to work.
That's all I got for now, I hope this was helpful.

