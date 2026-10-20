# Test UrTV's tools on a TV: the prompt for your AI agent

A quick check that the tools work with your TV and computer, for the developer. Nothing on the TV changes: no
installs, nothing disabled, no settings (only a tiny test file, deleted right after). Paste this to an agent that
runs commands on a computer on the same Wi-Fi as the TV (Codex: choose «Approve for me»):

> Test my TV with https://github.com/tshi-dev/UrTV: clone it (or `git pull` if it's here) and follow `docs/TEST-PROMPT.md`. You are on a computer on the same Wi-Fi as the TV.

---

Test UrTV's tools on my TV and write a report for the developer. Don't change anything on the TV: no installs,
nothing disabled, no settings, no reset. Talk to me in Arabic; keep commands in English.

- Windows: run every command in Git Bash: `"C:\Program Files\Git\bin\bash.exe" -lc "cd <folder> && <command>"`.
  Codex: run Git Bash outside the sandbox (its sandbox can't start it).
- One command at a time. If one fails, note its output and go on to the next. Don't fix anything, and don't write
  your own scripts.

In the UrTV folder:

1. `optimize/setup.sh`: must end with «✓ Python ADB client».
2. `optimize/find-tv.sh --check`, then `optimize/find-tv.sh`.
3. Tell me to watch the TV and press Allow (tick «Always allow»), then without waiting for my reply:
   `optimize/venv/bin/python -I optimize/tv.py --probe`
4. An upload through Git Bash:
   ```
   echo urtv-test > optimize/urtv-test.txt
   optimize/venv/bin/python -I optimize/tv.py --push-b64 optimize/urtv-test.txt /data/local/tmp/urtv-test.txt
   optimize/venv/bin/python -I optimize/tv.py "cat /data/local/tmp/urtv-test.txt; rm /data/local/tmp/urtv-test.txt"
   rm optimize/urtv-test.txt
   ```
5. Arabic output: `optimize/venv/bin/python -I optimize/tv.py "echo مرحبا"`
6. Club search, one call each: `optimize/saver.sh find-team الهلال`, then `السعودية`, `النصر`, `ريال مدريد`.
7. The screensaver from ADB: `optimize/start-saver.sh` (prints «dream» or «not started»), then
   `optimize/venv/bin/python -I optimize/tv.py "input keyevent KEYCODE_HOME"`.
8. `optimize/shot.sh home`, and look at the picture.
9. The apps a home screen can list (read only):
   ```
   optimize/venv/bin/python -I optimize/tv.py "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER"
   optimize/venv/bin/python -I optimize/tv.py "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER"
   optimize/venv/bin/python -I optimize/tv.py "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
   ```

Then write one report, in English, that I can paste to the developer:
- you (agent and model), the computer's system, and how long the whole test took;
- the TV's maker, model and Android version from step 3;
- each step 1–9: OK or FAILED, with the output lines that matter: every `!!` line and error, «dream» or «not
  started», the find-team results;
- step 9: the packages in the LAUNCHER list that are not in the LEANBACK_LAUNCHER one (the home screen will show
  them too), and the HOME list;
- anything slow, confusing, or that you had to retry.

Leave out the TV's IP address, my user name and folder paths. Save it as `optimize/run/test-report.md` and show it
to me.
