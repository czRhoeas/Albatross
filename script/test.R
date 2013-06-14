# Compare original and sampled networks.
# v1
# 
# Author: Vincent Labatut 06/2013
# source("C:/Eclipse/workspaces/Networks/Albatross/script/test.R")
###############################################################################

# the igraph library must have been installed, thanks to the following R function
# install.packages("igraph")
library("igraph")
folder <- "C:/Eclipse/workspaces/Networks/Albatross/"

# retrieve function
source.folder <- paste(folder,"script/",sep="")
source(paste(source.folder,"log-histogram.R",sep=""))


# read both graphs
data.folder <- paste(folder,"data/",sep="")
full.file <- paste(data.folder,"kdd03.net",sep="")
g.full <- read.graph(full.file, format="pajek")
sample.file <- paste(data.folder,"AS_sample.net",sep="")
g.sample <- read.graph(sample.file, format="pajek")


# compare sizes
cat("number of nodes: ",vcount(g.full)," vs. ",vcount(g.sample),"\n",sep="")
cat("number of links: ",ecount(g.full)," vs. ",ecount(g.sample),"\n",sep="")

# compare degrees
deg.all.full <- degree(g.full,mode="all")
deg.all.sample <- degree(g.sample,mode="all")
par(mfrow=c(1,2))
myhist(deg.all.full,main="All degree for full graph",log="y")
myhist(deg.all.sample,main="All degree for sampled subgraph",log="y")
cat("average all degree: ",mean(deg.all.full)," vs. ",mean(deg.all.sample),"\n",sep="")
deg.in.full <- degree(g.full,mode="in")
deg.in.sample <- degree(g.sample,mode="in")
dev.new()
par(mfrow=c(1,2))
myhist(deg.in.full,main="In degree for full graph",log="y")
myhist(deg.in.sample,main="In degree for sampled subgraph",log="y")
cat("average in degree: ",mean(deg.in.full)," vs. ",mean(deg.in.sample),"\n",sep="")
deg.out.full <- degree(g.full,mode="out")
deg.out.sample <- degree(g.sample,mode="out")
dev.new()
par(mfrow=c(1,2))
myhist(deg.out.full,main="Out degree for full graph",log="y")
myhist(deg.out.sample,main="Out degree for sampled subgraph",log="y")
cat("average out degree: ",mean(deg.out.full)," vs. ",mean(deg.out.sample),"\n",sep="")


# compare distances
#cat("average distance: ",average.path.length(g.full,directed=FALSE)," vs. ",average.path.length(g.sample,directed=FALSE),"\n",sep="")
dev.new()
par(mfrow=c(1,2))
tab <- as.table(path.length.hist(g.full)$res)
names(tab) <- 1:length(tab)
barplot(tab)
tab <- as.table(path.length.hist(g.sample)$res)
names(tab) <- 1:length(tab)
barplot(tab)

# compare clustering coefficient
cat("global transitivity: ",transitivity(g.full,type="global")," vs. ",transitivity(g.sample,type="global"),"\n",sep="")
trans.full <- transitivity(g.full,type="local")
trans.sample <- transitivity(g.sample,type="local")
dev.new()
par(mfrow=c(1,2))
myhist(trans.full,main="Local transitivity \nfor full graph",log="y")
myhist(trans.sample,main="Local transitivity \nfor sampled subgraph",log="y")

# compare modularity
#cat("modularity: ",modularity(walktrap.community(g.full))," vs. ",modularity(walktrap.community(g.sample)),"\n",sep="")
