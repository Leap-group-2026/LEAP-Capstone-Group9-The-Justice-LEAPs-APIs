from sqlalchemy import create_engine
from datetime import datetime,timedelta
import pandas as pd
from dotenv import load_dotenv
import os

load_dotenv()
engine = create_engine(f"postgresql+psycopg2://{os.getenv('DB_USERNAME')}:{os.getenv('DB_PASSWORD')}@localhost:15432/leap_projectdb")


def get_daily_completed_trades(start_date=None, end_date=None):
    if end_date is None:
        end_date = datetime.now()
    if start_date is None:
        start_date = datetime.now() - timedelta(days=30)
    
    query = """
    SELECT 
        DATE(created_at) as date,
        COUNT(*) as completed_orders_count
    FROM orders
    WHERE status = 'FILLED'
        AND created_at >= '{}'
        AND created_at <= '{}'
    GROUP BY DATE(created_at)
    ORDER BY DATE(created_at) ASC
    """.format(start_date.strftime('%Y-%m-%d'), end_date.strftime('%Y-%m-%d'))
    
    daily_trades = pd.read_sql(query, engine)
    return daily_trades


df = get_daily_completed_trades()
print(df)
print(df.info())
print(df.head(10))