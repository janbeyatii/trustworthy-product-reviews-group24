# What do I want to do?

Well, I want to be able to recommend users to other users thanks to their Jaccard distance

# What do I need to do to accomplish this?

I first need to calculate their Jaccard distance (for the time being, I will do this on demand, but in the future, this should be throttled to only be recalculated once in a while, say once per day.)

We can also be clever about this: to start, we should only calculate Jaccard distances for users that have reviewed at least one product in common.

## So how do you compare two reviews to decide if they are "the same" or "different"?

We can change this, but for now, let's say two reviews are "the same" if their scores are within 1 star of each other. Can I represent all of that *in* a SQL query?

I think I can?

## How should I be storing these results?

Obviously I don't want to recalculate all of these every single time. They should be stored. But how?

I think what I'll probably do is store the top 10 User IDs and associated Jaccard scores.

**Update:** Okay that schema doesn't look how I was expecting at all, there actually isn't any table mapping reviews to users. Reviews are their own thing, using products as a foreign key. I think I need to either:

a) work out a different way to compute a Jaccard index

b) create a user-review table that I can use for this

c) Just do a gnaryl SQL union

Remember what our goal is: find users with similar opinions to the current user.

So we start by selecting all the reviews which share a UID with the current user.

Then, we iterate over each of those reviews, which gets us a list of all the *other* UIDs which have reviewed these products.

Or. No. Maybe what we do is 

SELECT * from public.product_reviews where public.product_reviews.product_id IN (SELECT public.product_reviews.product_id from public.product_reviews WHERE public.product_reviews.uid = ?)





And that will give us a list of *ALL* reviews for products we have reviews, including our own. I'm not 100% certain about how we recieve that data as code.

Now remember, we want to count 

From there, we want to iterate over 
