
rssi <- c(-78, -79, -80, -82, -88, -93, -94)
length(rssi)
rss0 <- -64
result.n <- c()
for(i in 1:length(rssi)){
  
  result <-((rss0) - (rssi[i]))/(10*log10(i+1))
  result.n <- c(result.n, result)
}

mean(result.n[-1])