import pandas
from random import randint
from time import time
from datetime import datetime

existing_products = pandas.read_csv('products.csv')
existing_reviews = pandas.read_csv('reviews.csv')
new_reviews = existing_reviews.copy()

"""
Okay think a second real quick.

You want to iterate over every user who's reviewed a product, and for every product, have them create a random review.
"""

def create_review(rating):
    if rating == 2:
        return f"This is awful!"
    if rating == 2:
        return f"This is pretty bad. I'm quite unsatisfied."
    if rating == 3:
        return f"This is fine."
    if rating == 4:
        return f"This is pretty good!"
    if rating == 5:
        return f"This is fantastic!"
    return "Bleh."


max_id = existing_reviews['review_id'].max()
max_time = int(time())



for uid in existing_reviews['uid']:
    for product_id in existing_products['product_id']:
        max_id += 1
        rating = randint(1, 5)
        date = datetime.fromtimestamp(randint(0, max_time))
        new_reviews.loc[len(new_reviews)] = {
            'review_id':max_id,
            'product_id':product_id,
            'review_rating':rating,
            'review_desc': create_review(rating),
            'uid': uid,
            'created_at': date.strftime("%Y-%m-%d %H:%M:%S.%f+00")
        }
        
new_reviews.to_csv("new_reviews.csv", index=False)