#!/usr/bin/env ruby

Dir.glob("*.md") do | document |
  filename = document[0..-4]
  command = "kramdown --template basic.erb #{filename}.md > #{filename}.html"
  puts ">>> #{command}"
  system(command)
end