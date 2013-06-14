# Plots histograms, possible on a logarithmic scale
# (which the original hist function does not allow). 
# 
# 
# Author: chrispy 12/2011
# Source: http://stackoverflow.com/questions/1245273/histogram-with-logarithmic-scale
# source("C:/Eclipse/workspaces/Networks/Albatross/script/histogram.R")
###############################################################################

myhist <- function(x, ..., breaks="Sturges",
		main = paste("Histogram of", xname),
		xlab = xname,
		ylab = "Frequency") {
	xname = paste(deparse(substitute(x), 500), collapse="\n")
	h = hist(x, breaks=breaks, plot=FALSE)
	plot(h$breaks, c(NA,h$counts), type='S', main=main,
			xlab=xlab, ylab=ylab, axes=FALSE, ...)
	axis(1)
	axis(2)
	lines(h$breaks, c(h$counts,NA), type='s')
	lines(h$breaks, c(NA,h$counts), type='h')
	lines(h$breaks, c(h$counts,NA), type='h')
	lines(h$breaks, rep(0,length(h$breaks)), type='S')
	invisible(h)
}
