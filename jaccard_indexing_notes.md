# What do I want to do?

Well, I want to be able to recommend users to other users thanks to their Jaccard distance

# What do I need to do to accomplish this?

I first need to calculate their Jaccard distance (for the time being, I will do this on demand, but in the future, this should be throttled to only be recalculated once in a while, say once per day.)

We can also be clever about this: to start, we should only calculate Jaccard distances for users that have reviewed at least one product in common.

## So how do you compare two reviews to decide if they are "the same" or "different"?

We can change this, but for now, let's say two reviews are "the same" if their scores are within 1 star of each other. Can I represent all of that *in* a SQL query?

I think I can?

## How should I be storing these results?

Obviously I don't want to recalculate all of these every single time. They should be stored. 
