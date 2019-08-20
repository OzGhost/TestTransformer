#!/bin/sh

base=$1

if [ -z $base ]; then
    echo "Please provide base path as paramter!";
    exit;
fi;

ufold="$base/unittest";
bfold="$base/jmocktest";

if [ -d $ufold ]; then
    echo "found test folder at: $ufold";
    if [ -d $bfold ]; then
        echo "Clean up $bfold"
        rm -rf $bfold;
    fi;
    cp -R $ufold $bfold
    for i in `find $bfold -type f`; do
        rm $i;
    done;
    find $ufold -type f -iname "*.java" > the_input_tests
else
    echo "Given folder $base doesn't have folder 'unittest' as direct child";
    exit;
fi;

