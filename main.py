#!/usr/bin/env python
import subprocess

def runProcess(exe):
    p = subprocess.Popen(exe, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,cwd='/Users/kel/data/into-cps/tracability-test')
    while (True):
        retcode = p.poll()  # returns None while subprocess is running
        line = p.stdout.readline().rstrip('\n')
        yield line
        if (retcode is not None):

            line = p.stdout.readline().rstrip('\n')
            yield line


            break


for line in runProcess('git rev-list master'.split()):
    print line,
