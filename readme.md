# INTO-CPS Tracability for Overture


## How to 

To run a full repo sync use the following command:

```bash
java -jar tracability-driver-*-jar-with-dependencies.jar -host http://localhost:8083 -s  --dry-run -vdm -exclude SysML -repo /path/to/repo
```

or to sync a single commit only:

```bash
java -jar tracability-driver-*-jar-with-dependencies.jar -host http://localhost:8083 -c <commit-hash> --dry-run -vdm -exclude SysML -repo /path/to/repo
```

The `--dry-run` command makes the tool only print the data that would be send. Drop this so send the messages. If a custom host is used then specify `-host <url>` to override the default host URL.

### Git hook

The tool can be used in combination with git hooks. If the daemon is running locally such a hook could look like:

Hook file: `.git/hooks/post-commit`

```bash
#!/bin/sh
java -jar tracability-driver-*-jar-with-dependencies.jar -c HEAD -vdm -exclude SysML -repo $GIT_DIR../
```

assuming that the `*.jar` file is also in hooks. 

Make sure it is executable:

```bash
chmod +x .git/hooks/post-commit
```

This will trigger a sync of the new commit to the daemon.
