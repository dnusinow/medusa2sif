# medusa2sif

A program designed to convert Medusa
(https://sites.google.com/site/medusa3visualization/) network files,
as output by the STRING (http://string.embl.de/) program to Simple
Interaction Format (SIF) supported by Cytoscape
(http://cytoscape.org/).

## Usage

java -jar medusa2sif.jar [-i] in.medusa [-o] out.sif

Arguments
---------
-i/--input - path to input Medusa file
-o/--output - path to output SIF file
-t/--type - interaction type for edges

Arguments may be specified using the command switches or positionally, but not a mix.

## License

Copyright © 2013 David Nusinow <david@gravitypulls.net>

Distributed under the GNU General Public License (GPL) version 3.0.
