#!/bin/bash --login
#SBATCH --job-name=pre_final_run_test         # name of the run
#SBATCH --output=greedy/Mo/pre_final_run_test.log              # logfile of the run
#SBATCH --nodes=1                      # Run on a single computer
#SBATCH --ntasks=1                     # Run a single task        
#SBATCH --cpus-per-task=41             # Number of CPU cores per task
#SBATCH --mem=64G                     # Job memory request
#SBATCH --time=96:00:00                # Time limit hrs:min:sec
#SBATCH --mail-type=BEGIN,END,FAIL     # Send email on begin, end, and fail
#SBATCH --mail-user=milo.bennett@tu-berlin.de   # Your email address


module load java/21


java -Xmx64G -jar zeroCUTS-0.0.1-pre_final_run_test.jar --day MO --outputLocation greedy/Mo --clusterStrategy seeding --jspritIterations 10000

chmod 770 -R .
