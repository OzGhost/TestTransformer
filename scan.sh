#!/bin/sh

base=$1

if [ -z $base ]; then
    echo "Please provide base path as paramter!";
    exit;
fi;

if [ -d $base ]; then
    find $base -name "*.java" | grep -v "Test.java" > scan_out
else
    echo "Given folder '" $base "' does not exists!"
    exit;
fi;

