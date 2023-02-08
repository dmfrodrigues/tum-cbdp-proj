#!/bin/python

from sys import argv

if len(argv) != 2:
    print("Usage: {} <test_script>".format(argv[0]))
    exit(1)


if __name__ == "__main__":
    print("This is a test !! " + argv[1].split().join("-"))
