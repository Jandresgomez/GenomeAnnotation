# GenomeAnnotation
Genome annotation for prokaryote genomes via HHM and dynamic programming.

There are three tools within this project.
1) FlipFasta
Takes a fasta's filePath as an argument and generates a %fileName%.flip file which contains the reverse complement of said fasta in fasta format.
2) ModelLearner
Takes multiple fasta file name prefixes that must reside inside the ./Data/ folder, e.g. Ecoli Aeropyrum for ./Data/Ecoli.fasta and ./Aeropyrum.fasta. Writes a model trained from said genomes to the ./Results/model.hmm file.
3) GenePredictor
Takes 3 parameters. First, a file name prefix for the genome to annotate that must reside inside the ./Data/ folder. Second, a process name of your choice (e.g. EcoliPrediction). Third, a filePath to the model to use (e.g. ./Results/model.hmm). <br>
This will output 2 pseudo gff3 files to the ./Results/ folder, the _flip file cointains annotations for the reverse complement strand, and the other file contains annotations for the original strand. <br>
The .fasta.flip file must exist before running this tool, this can be generated with the FlipFasta tool. 

# Authors
* Jorge Andrés Gómez 
* Laura Carolina Camelo Valera
