import pandas as pd
import matplotlib.pyplot as plt
from sqlalchemy import create_engine
import os
from dotenv import load_dotenv

load_dotenv()

engine = create_engine(f"postgresql+psycopg2://{os.getenv('DB_USERNAME')}:{os.getenv('DB_PASSWORD')}@localhost:15432/leap_projectdb")

df = pd.read_sql('SELECT * FROM orders', engine)

#Getting orders for the previous 30 days
last_30_days = df[df['created_at'] >= pd.Timestamp.now() - pd.Timedelta(days=30)]

#Getting the value of each order status 
def make_autopct(values):
    def my_autopct(pct):
        total = sum(values)
        val = int(round(pct*total/100.0))
        return f'{pct:.1f}% ({val:d})'
    return my_autopct

#Plotting the pie chart with the percentage and number of each order status of the orders from the last 30 days
plt.figure(figsize=(10, 8))
per_status_count = last_30_days['status'].value_counts()
per_status_count.plot(kind='pie', autopct=make_autopct(per_status_count), startangle=90)
plt.title('Order Statuses in the Last 30 Days')
plt.ylabel('')
plt.show()