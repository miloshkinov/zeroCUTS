#!/bin/bash --login
#SBATCH --job-name=testcarriersplit               # name of the run
#SBATCH --output=testcarriersplit.log              # logfile of the run
#SBATCH --nodes=1                      # Run on a single computer
#SBATCH --ntasks=1                     # Run a single task        
#SBATCH --cpus-per-task=12             # Number of CPU cores per task
#SBATCH --mem=60G                     # Job memory request
#SBATCH --time=24:00:00                # Time limit hrs:min:sec
#SBATCH --mail-type=BEGIN,END,FAIL     # Send email on begin, end, and fail
#SBATCH --mail-user=milo.bennett@tu-berlin.de   # Your email address


module load java/21


java -Xmx180G -jar zeroCUTS-0.0.1-split.jar

chmod 770 -R .
